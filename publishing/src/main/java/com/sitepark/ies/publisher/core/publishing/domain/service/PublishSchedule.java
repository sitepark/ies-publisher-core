package com.sitepark.ies.publisher.core.publishing.domain.service;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList;
import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRetentionSetting;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishQueueStatus;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishingCapacity;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * Decides what is published when: the lists that were requested, their tasks waiting, the entities
 * being published, and the rules that keep parallel publishing correct.
 *
 * <p>Whatever is published is requested as a list - one entity an editor saves, the entities
 * following a change, a whole site. A list has an id and a name, says how urgent it is and which
 * lists it has to wait for, and can be watched and cancelled on its own.
 *
 * <p>The schedule owns no thread. Whoever dispatches asks it for the next task and blocks until
 * one may run; whoever publishes in the foreground asks it for the entity and blocks until it is
 * free. All state is guarded by one lock that is never held while anything is published.
 *
 * <p>The rules:
 *
 * <ul>
 *   <li>An entity is published in a channel by one thread at a time, background or foreground.
 *   <li>A task requested by a list for an entity that already has a task waiting is merged into
 *       that task. It keeps its place, belongs to both lists and is as urgent as the more urgent
 *       one.
 *   <li>A request for an entity being published waits and runs afterwards: the running publication
 *       may have read the state before the change.
 *   <li>The tasks of a list wait for the other tasks of the lists it comes after.
 *   <li>A cleanup runs alone, once no other task is waiting and nothing is being published. No
 *       background task starts while it runs.
 *   <li>While a publication runs in the foreground, fewer background tasks are started.
 *   <li>A foreground publication takes over a waiting task of its entity if that does the same
 *       work and asks for no more, options and attachment included. A task that asks for more, or
 *       for something else - revoking instead of publishing - stays and runs afterwards, as does
 *       one that waits for another list, which the foreground cannot know to be done.
 *   <li>Cancelling a list drops its waiting tasks, unless another list wants them too. What is
 *       being published is finished.
 * </ul>
 *
 * <p>Merging can make lists wait for each other: a task of one list that also belongs to a list
 * coming after it. No order satisfies both. When nothing can run for that reason and nothing is
 * being published, the oldest such task runs.
 *
 * <p>A thread publishing one entity may publish another one in the foreground. Two threads doing so
 * for each other's entity would wait forever; nothing in the publisher does.
 */
@Singleton
// The rules work on the waiting tasks, holds and lists, which are state of this package only.
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.LawOfDemeter"})
public class PublishSchedule {

  /** How often a task is tried again after an attempt that failed for a passing reason. */
  private static final int MAX_RETRIES = 1;

  /** How long a dispatcher waits at most before it asks for the capacity again, in milliseconds. */
  private static final long RECHECK_MILLIS = 1000;

  private final ReentrantLock lock = new ReentrantLock();

  private final Condition changed = this.lock.newCondition();

  /** The tasks waiting to be published. */
  private final WaitingTasks waiting = new WaitingTasks();

  /** Who publishes what, who waits for it in the foreground, and who suspended publishing. */
  private final EntityHolds holds = new EntityHolds();

  /** The lists going on and the ones over. */
  private final PublishLists publishLists;

  /** Tells the time the tasks of a list start. */
  private final Clock clock;

  private long processed;

  private long failed;

  /** Whether the dispatcher was told that everything is done, since the last request. */
  private boolean drainedAnnounced = true;

  /**
   * Creates a schedule that keeps the lists that are over as {@link
   * PublishListRetentionSetting#DEFAULT} until it is told otherwise by {@link #retain}.
   */
  @Inject
  public PublishSchedule() {
    this(Clock.systemUTC());
  }

  /**
   * Creates a schedule that tells the time by a clock of its own, for tests.
   *
   * @param clock the clock
   */
  PublishSchedule(Clock clock) {
    this.clock = clock;
    this.publishLists = new PublishLists(clock);
  }

  // --- lists

  /**
   * Sets how many of the lists that are over are kept, and for how long. Applies at once: lists
   * beyond the new setting are forgotten.
   *
   * @param retention the setting
   */
  public void retain(PublishListRetentionSetting retention) {
    this.lock.lock();
    try {
      this.publishLists.retain(retention);
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Requests a list of tasks, all at once: none of them is handed out before all are known.
   *
   * @param request the list
   * @return the id the list can be watched, waited for and cancelled by
   */
  public PublishListId open(PublishListRequest request) {
    this.lock.lock();
    try {
      ListState list = this.publishLists.register(request, false);
      request.tasks().forEach(task -> this.waiting.add(task, list));
      if (!list.isOver()) {
        this.drainedAnnounced = false;
      }
      this.publishLists.finishIfOver(list);
      this.changed.signalAll();
      return list.id;
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Registers a list whose tasks the calling thread publishes itself, in the foreground. The list
   * can be watched like any other; its tasks are handed to {@link #enterForeground(PublishTask,
   * PublishListId)} one by one, and {@link #closeForeground} ends it.
   *
   * @param request the list
   * @return the id of the list
   */
  public PublishListId openForeground(PublishListRequest request) {
    this.lock.lock();
    try {
      ListState list = this.publishLists.register(request, true);
      list.expectRequestedTasks();
      this.publishLists.finishIfOver(list);
      return list.id;
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Ends a foreground list: tasks the thread never got to count as cancelled.
   *
   * @param id the list
   */
  public void closeForeground(PublishListId id) {
    this.lock.lock();
    try {
      ListState list = this.publishLists.going(id);
      if (list == null || !list.foreground) {
        return;
      }
      list.cancelNeverStarted();
      this.publishLists.finishIfOver(list);
      this.changed.signalAll();
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Waits until no task of the list is waiting or running any more - whatever else is requested
   * meanwhile. Interrupting the waiting thread does not take the tasks back.
   *
   * @param id the list
   * @return the list as it ended
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws IllegalArgumentException if no such list is known
   */
  public PublishList await(PublishListId id) throws InterruptedException {
    this.lock.lockInterruptibly();
    try {
      ListState list = this.publishLists.find(id);
      if (list == null) {
        throw new IllegalArgumentException("unknown list: " + id);
      }
      while (!list.isFinished()) {
        this.changed.await();
      }
      return list.snapshot();
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Returns a list as it stands.
   *
   * @param id the list
   * @return the list, or empty if it is unknown or was finished too long ago
   */
  public Optional<PublishList> list(PublishListId id) {
    this.lock.lock();
    try {
      return Optional.ofNullable(this.publishLists.find(id)).map(ListState::snapshot);
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Returns the lists with tasks waiting or running, oldest first, followed by the lists finished
   * most recently, youngest first.
   *
   * @return the lists as they stand
   */
  public List<PublishList> lists() {
    this.lock.lock();
    try {
      return this.publishLists.snapshots();
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Cancels a list: its waiting tasks are dropped, unless another list wants them too. Tasks being
   * carried out are finished.
   *
   * @param id the list
   * @return {@code false} if no such list has tasks waiting or running
   */
  public boolean cancel(PublishListId id) {
    this.lock.lock();
    try {
      ListState list = this.publishLists.going(id);
      if (list == null) {
        return false;
      }
      this.cancel(list);
      this.changed.signalAll();
      return true;
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Cancels every list: all waiting tasks are dropped. Tasks being carried out are finished.
   *
   * @return the number of lists cancelled
   */
  public int cancelAll() {
    this.lock.lock();
    try {
      List<ListState> active = this.publishLists.going();
      active.forEach(this::cancel);
      this.changed.signalAll();
      return active.size();
    } finally {
      this.lock.unlock();
    }
  }

  // --- background

  /**
   * Waits for the next task that may run in the background and marks its entity as being
   * published. Whoever takes a task has to report its end with {@link #completed}.
   *
   * @param capacity how much may run at the same time; asked again whenever the schedule looks for
   *     a task, as it may change while waiting
   * @return the task, or empty once everything requested is done; told once per run of work
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public Optional<PublishTask> awaitNext(Supplier<PublishingCapacity> capacity)
      throws InterruptedException {
    this.lock.lockInterruptibly();
    try {
      while (true) {
        if (!this.holds.isSuspended()) {
          WaitingTask next = this.findRunnable(capacity.get());
          if (next != null) {
            this.start(next);
            return Optional.of(next.task);
          }
          if (!this.drainedAnnounced
              && this.waiting.isEmpty()
              && this.holds.runningInBackground() == 0) {
            this.drainedAnnounced = true;
            return Optional.empty();
          }
        }
        // the capacity may change without anybody telling
        this.changed.await(RECHECK_MILLIS, TimeUnit.MILLISECONDS);
      }
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Returns the next task that may run in the background right now, like {@link #awaitNext} but
   * without waiting.
   *
   * @param capacity how much may run at the same time
   * @return the task, or empty if none may run now
   */
  public Optional<PublishTask> pollNext(PublishingCapacity capacity) {
    this.lock.lock();
    try {
      if (this.holds.isSuspended()) {
        return Optional.empty();
      }
      WaitingTask next = this.findRunnable(capacity);
      if (next == null) {
        return Optional.empty();
      }
      this.start(next);
      return Optional.of(next.task);
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Tells that the calling thread is the one executing a task handed out. Lets the thread publish
   * the same entity again from within the task without waiting for itself.
   *
   * @param key the entity and channel of the task
   */
  public void adopt(PublishTaskKey key) {
    this.lock.lock();
    try {
      Hold hold = this.holds.get(key);
      if (hold != null && !hold.foreground) {
        hold.owner = Thread.currentThread();
      }
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Reports the end of a task handed out by {@link #awaitNext}. A task worth another attempt is
   * requested again, unless a newer request for its entity is waiting already.
   *
   * @param task the task
   * @param outcome how it ended
   */
  public void completed(PublishTask task, PublishOutcome outcome) {
    this.lock.lock();
    try {
      Hold hold = this.holds.get(task.key());
      if (hold == null || hold.foreground) {
        return;
      }
      this.holds.release(hold);
      this.processed++;
      hold.stopped();
      if (outcome == PublishOutcome.RETRY && task.attempt() < MAX_RETRIES) {
        // the lists that asked for the task wait for what follows it
        if (this.waiting.requeue(task, hold.lists)) {
          this.drainedAnnounced = false;
        }
      } else {
        if (outcome != PublishOutcome.DONE) {
          this.failed++;
        }
        this.settle(hold, outcome == PublishOutcome.DONE);
      }
      this.changed.signalAll();
    } finally {
      this.lock.unlock();
    }
  }

  // --- foreground

  /**
   * Waits until the entity of the task may be published in the foreground, without a list.
   *
   * @param task what the foreground is about to do
   * @see #enterForeground(PublishTask, PublishListId)
   */
  public void enterForeground(PublishTask task) {
    this.enterForeground(task, null);
  }

  /**
   * Waits until the entity of the task may be published in the foreground and marks it as being
   * published. A task waiting for the same work - the same type, asking for no more than the
   * foreground task does - is taken over, with the lists that want it, since the foreground does
   * it now; any other task stays, and so does one that waits for another list. Has to be paired with
   * {@link #leaveForeground}.
   *
   * @param task what the foreground is about to do
   * @param listId the foreground list the task belongs to, or {@code null}
   */
  public void enterForeground(PublishTask task, @Nullable PublishListId listId) {
    PublishTaskKey key = task.key();
    Thread thread = Thread.currentThread();
    this.lock.lock();
    try {
      ListState list = listId == null ? null : this.publishLists.going(listId);
      Hold own = this.holds.get(key);
      if (own != null && thread.equals(own.owner)) {
        own.depth++;
        own.add(list, this.clock.instant());
        return;
      }
      this.holds.startWaitingInForeground(key);
      while (this.holds.isHeld(key) || this.holds.isSuspendedFor(thread)) {
        this.changed.awaitUninterruptibly();
      }
      this.holds.stopWaitingInForeground(key);
      Hold hold = Hold.ofForeground(thread, task);
      hold.add(list, this.clock.instant());
      this.holds.put(hold);
      WaitingTask covered = this.waiting.takeOverBy(task);
      if (covered != null) {
        // whoever asked for the task is served by the foreground
        Instant now = this.clock.instant();
        covered.lists.forEach(coveredList -> hold.add(coveredList, now));
      }
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Ends a foreground publication.
   *
   * @param key the entity and channel
   * @param succeeded whether the publication ended without an error
   */
  public void leaveForeground(PublishTaskKey key, boolean succeeded) {
    this.lock.lock();
    try {
      Hold hold = this.holds.get(key);
      if (hold == null || !Thread.currentThread().equals(hold.owner)) {
        return;
      }
      if (hold.depth > Hold.OUTERMOST) {
        hold.depth--;
        return;
      }
      if (!hold.foreground) {
        return;
      }
      this.holds.release(hold);
      hold.stopped();
      this.settle(hold, succeeded);
      this.changed.signalAll();
    } finally {
      this.lock.unlock();
    }
  }

  // --- everything at once

  /**
   * Suspends publishing: waits until nothing is being published by another thread, and lets
   * nothing start until {@link #resume}. The calling thread itself may still publish in the
   * foreground.
   */
  public void suspend() {
    Thread thread = Thread.currentThread();
    this.lock.lock();
    try {
      this.holds.suspend(thread);
      while (this.holds.isPublishedByOthers(thread)) {
        this.changed.awaitUninterruptibly();
      }
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Resumes publishing after {@link #suspend}. Meant to be called by the thread that suspended. A
   * thread that did not suspend lifts the oldest suspension instead, so a suspension is not kept
   * for good by a thread that is gone.
   */
  public void resume() {
    this.lock.lock();
    try {
      this.holds.resume(Thread.currentThread());
      this.changed.signalAll();
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Returns whether a task of the type is waiting for the entity.
   *
   * @param key the entity and channel
   * @param type what the task does
   * @return {@code true} if such a task is waiting
   */
  public boolean isWaiting(PublishTaskKey key, PublishTaskType type) {
    this.lock.lock();
    try {
      return this.waiting.isWaiting(key, type);
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Returns whether the calling thread is in the middle of a publication, as a worker executing a
   * task or in the foreground. Such a thread must not wait for other tasks: it may be the only
   * worker there is.
   *
   * @return {@code true} if the thread publishes right now
   */
  public boolean isPublishing() {
    Thread thread = Thread.currentThread();
    this.lock.lock();
    try {
      return this.holds.isPublishing(thread);
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Returns where publishing stands.
   *
   * @return the status
   */
  public PublishQueueStatus status() {
    this.lock.lock();
    try {
      return new PublishQueueStatus(
          this.waiting.size(),
          this.holds.runningInBackground(),
          this.holds.activeInForeground(),
          this.processed,
          this.failed,
          this.holds.isSuspended());
    } finally {
      this.lock.unlock();
    }
  }

  // --- internally

  /** Drops the waiting tasks of the list, unless another list wants them too. */
  private void cancel(ListState list) {
    this.waiting.withdraw(list);
    this.publishLists.finishIfOver(list);
  }

  /** The work is over for good: tells every list that asked for it. */
  private void settle(Hold hold, boolean succeeded) {
    for (ListState list : hold.lists) {
      list.taskEnded(hold.task, succeeded);
      this.publishLists.finishIfOver(list);
    }
  }

  private @Nullable WaitingTask findRunnable(PublishingCapacity capacity) {
    if (!this.holds.mayStartBackground(capacity)) {
      return null;
    }
    CleanupCounts counts = this.waiting.cleanupCounts();
    for (WaitingTask waitingTask : this.waiting.inOrder()) {
      if (this.holds.isFree(waitingTask.task.key()) && !waitingTask.hasToWait(counts)) {
        return waitingTask;
      }
    }
    if (!this.holds.isEmpty()) {
      return null;
    }
    if (this.waiting.onlyCleanups()) {
      return this.waiting.oldestCleanup();
    }
    return this.oldestWaitingInACircle();
  }

  /**
   * Nothing is being published and nothing may start: the waiting tasks wait for lists that in turn
   * wait for them, which merging can cause. The first of them runs, so the rest can follow. A task
   * waiting for a foreground list is not in a circle: that list is published by its own thread.
   */
  private @Nullable WaitingTask oldestWaitingInACircle() {
    for (WaitingTask waitingTask : this.waiting.inOrder()) {
      if (this.holds.isFree(waitingTask.task.key()) && !waitingTask.waitsForForeground()) {
        return waitingTask;
      }
    }
    return null;
  }

  private void start(WaitingTask waitingTask) {
    this.waiting.remove(waitingTask);
    Hold hold = Hold.ofBackground(waitingTask);
    Instant now = this.clock.instant();
    waitingTask.lists.forEach(list -> hold.add(list, now));
    this.holds.put(hold);
  }
}
