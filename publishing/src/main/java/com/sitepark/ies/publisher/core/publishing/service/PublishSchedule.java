package com.sitepark.ies.publisher.core.publishing.service;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList;
import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishQueueStatus;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.port.PublishListRetention;
import com.sitepark.ies.publisher.core.publishing.port.PublishingLimits;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
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
 *   <li>A foreground publication takes over a waiting task of its entity, unless that task waits
 *       for another list, which the foreground cannot know to be done.
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
// The maps are guarded by the lock; entries, holds and lists are private state of this class.
@SuppressWarnings({
  "PMD.TooManyMethods",
  "PMD.CyclomaticComplexity",
  "PMD.CouplingBetweenObjects",
  "PMD.UseConcurrentHashMap",
  "PMD.LawOfDemeter",
  "PMD.ExcessiveImports"
})
public class PublishSchedule {

  /** How often a task is tried again after an attempt that failed for a passing reason. */
  private static final int MAX_RETRIES = 1;

  /** How long a dispatcher waits at most before it looks at the limits again, in milliseconds. */
  private static final long RECHECK_MILLIS = 1000;

  /** The depth of a publication that is not nested in another one of the same entity. */
  private static final int OUTERMOST = 1;

  private static final DateTimeFormatter ID_TIME =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

  private static final Comparator<Entry> BY_SEQUENCE = Comparator.comparingLong(e -> e.sequence);

  private static final Comparator<Entry> BY_PRIORITY =
      Comparator.<Entry, PublishPriority>comparing(e -> e.priority).thenComparing(BY_SEQUENCE);

  private final ReentrantLock lock = new ReentrantLock();

  private final Condition changed = this.lock.newCondition();

  /** The waiting tasks by entity and place. */
  private final Map<SlotKey, Entry> waiting = new HashMap<>();

  /** The waiting tasks except cleanups, in the order they are handed out. */
  private final NavigableSet<Entry> queue = new TreeSet<>(BY_PRIORITY);

  /** The waiting cleanups, oldest first. */
  private final NavigableSet<Entry> cleanups = new TreeSet<>(BY_SEQUENCE);

  /** The entities being published. */
  private final Map<PublishTaskKey, Hold> holds = new HashMap<>();

  /** The foreground publications waiting for an entity, by entity. */
  private final Map<PublishTaskKey, Integer> foregroundWaiting = new HashMap<>();

  /** The threads that suspended publishing and have not resumed it. */
  private final List<Thread> suspenders = new ArrayList<>();

  /** The lists with tasks waiting or running, in the order they were requested. */
  private final Map<PublishListId, ListState> openLists = new LinkedHashMap<>();

  /** The lists that are over, youngest first. */
  private final Deque<ListState> finishedLists = new ArrayDeque<>();

  /** How many of the lists that are over are kept, and for how long. */
  private final PublishListRetention retention;

  /** Makes the ids of lists unique across restarts: the time this schedule was created. */
  private final String idPrefix = ID_TIME.format(Instant.now());

  private long listCounter;

  private long sequence;

  private int runningBackground;

  /** Whether a cleanup runs; nothing else is started in the background meanwhile. */
  private boolean cleaningUp;

  private int foregroundActive;

  private long processed;

  private long failed;

  /** Whether the dispatcher was told that everything is done, since the last request. */
  private boolean drainedAnnounced = true;

  /** Creates a schedule that keeps the lists that are over as {@link PublishListRetention#DEFAULT}. */
  public PublishSchedule() {
    this(PublishListRetention.DEFAULT);
  }

  /**
   * Creates a schedule.
   *
   * @param retention how many of the lists that are over are kept, and for how long
   */
  @Inject
  public PublishSchedule(PublishListRetention retention) {
    this.retention = retention;
  }

  // --- lists

  /**
   * Requests a list of tasks, all at once: none of them is handed out before all are known.
   *
   * @param request the list
   * @return the id the list can be watched, waited for and cancelled by
   */
  public PublishListId open(PublishListRequest request) {
    this.lock.lock();
    try {
      ListState list = this.register(request, false);
      for (PublishTask task : request.tasks()) {
        Entry entry = this.entryFor(task);
        this.attach(entry, list);
        this.index(entry);
      }
      if (list.open == 0) {
        this.finish(list);
      } else {
        this.drainedAnnounced = false;
      }
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
      ListState list = this.register(request, true);
      list.total = request.tasks().size();
      list.open = list.total;
      if (list.open == 0) {
        this.finish(list);
      }
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
      ListState list = this.openLists.get(id);
      if (list == null || !list.foreground) {
        return;
      }
      int neverStarted = list.open - list.running;
      list.open -= neverStarted;
      list.cancelled += neverStarted;
      if (list.open == 0) {
        this.finish(list);
      }
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
      ListState list = this.find(id);
      if (list == null) {
        throw new IllegalArgumentException("unknown list: " + id);
      }
      while (list.finished == null) {
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
      return Optional.ofNullable(this.find(id)).map(ListState::snapshot);
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
      this.dropExpiredLists();
      List<PublishList> all = new ArrayList<>(this.openLists.size() + this.finishedLists.size());
      this.openLists.values().forEach(list -> all.add(list.snapshot()));
      this.finishedLists.forEach(list -> all.add(list.snapshot()));
      return all;
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
      ListState list = this.openLists.get(id);
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
      List<ListState> active = new ArrayList<>(this.openLists.values());
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
   * @param limits how much may run at the same time
   * @return the task, or empty once everything requested is done; told once per run of work
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public Optional<PublishTask> awaitNext(PublishingLimits limits) throws InterruptedException {
    this.lock.lockInterruptibly();
    try {
      while (true) {
        if (this.suspenders.isEmpty()) {
          Entry next = this.findRunnable(limits);
          if (next != null) {
            this.start(next);
            return Optional.of(next.task);
          }
          if (!this.drainedAnnounced && this.waiting.isEmpty() && this.runningBackground == 0) {
            this.drainedAnnounced = true;
            return Optional.empty();
          }
        }
        // the limits may change without anybody telling
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
   * @param limits how much may run at the same time
   * @return the task, or empty if none may run now
   */
  public Optional<PublishTask> pollNext(PublishingLimits limits) {
    this.lock.lock();
    try {
      if (!this.suspenders.isEmpty()) {
        return Optional.empty();
      }
      Entry next = this.findRunnable(limits);
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
      this.holds.remove(task.key());
      this.runningBackground--;
      if (task.type() == PublishTaskType.CLEANUP) {
        this.cleaningUp = false;
      }
      this.processed++;
      hold.lists.forEach(list -> list.running--);
      if (outcome == PublishOutcome.RETRY && task.attempt() < MAX_RETRIES) {
        this.tryAgain(hold, task);
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
   * published. A task waiting for the same work is taken over, with the lists that want it, since
   * the foreground does it now; a task that waits for another list stays. Has to be paired with
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
      ListState list = listId == null ? null : this.openLists.get(listId);
      Hold own = this.holds.get(key);
      if (own != null && thread.equals(own.owner)) {
        own.depth++;
        own.add(list);
        return;
      }
      this.foregroundActive++;
      this.foregroundWaiting.merge(key, 1, Integer::sum);
      while (this.holds.containsKey(key) || this.isSuspendedFor(thread)) {
        this.changed.awaitUninterruptibly();
      }
      this.stopWaitingInForeground(key);
      Hold hold = Hold.ofForeground(thread, task);
      hold.add(list);
      this.holds.put(key, hold);
      Entry covered = this.waiting.get(SlotKey.of(task));
      if (covered != null && task.type() != PublishTaskType.CLEANUP && !covered.hasToWait()) {
        this.remove(covered);
        // whoever asked for the task is served by the foreground
        covered.lists.forEach(hold::add);
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
      if (hold.depth > OUTERMOST) {
        hold.depth--;
        return;
      }
      if (!hold.foreground) {
        return;
      }
      this.holds.remove(key);
      this.foregroundActive--;
      hold.lists.forEach(list -> list.running--);
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
      this.suspenders.add(thread);
      while (this.isPublishedByOthers(thread)) {
        this.changed.awaitUninterruptibly();
      }
    } finally {
      this.lock.unlock();
    }
  }

  /** Resumes publishing after {@link #suspend}. */
  public void resume() {
    this.lock.lock();
    try {
      if (!this.suspenders.remove(Thread.currentThread()) && !this.suspenders.isEmpty()) {
        this.suspenders.remove(0);
      }
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
      Entry entry = this.waiting.get(new SlotKey(key, type.slot()));
      return entry != null && entry.task.type() == type;
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
      return this.holds.values().stream().anyMatch(hold -> thread.equals(hold.owner));
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
          this.runningBackground,
          this.foregroundActive,
          this.processed,
          this.failed,
          !this.suspenders.isEmpty());
    } finally {
      this.lock.unlock();
    }
  }

  // --- lists, internally

  private ListState register(PublishListRequest request, boolean foreground) {
    this.listCounter++;
    PublishListId id =
        new PublishListId(this.idPrefix + "-" + String.format("%06d", this.listCounter));
    ListState list = new ListState(id, request, foreground);
    for (PublishListId predecessor : request.after()) {
      ListState known = this.openLists.get(predecessor);
      // a list that is over, or was never known, is nothing to wait for
      if (known != null) {
        list.after.add(known);
      }
    }
    this.openLists.put(id, list);
    return list;
  }

  private @Nullable ListState find(PublishListId id) {
    ListState active = this.openLists.get(id);
    if (active != null) {
      return active;
    }
    for (ListState finished : this.finishedLists) {
      if (finished.id.equals(id)) {
        return finished;
      }
    }
    return null;
  }

  private void finish(ListState list) {
    list.finished = Instant.now();
    list.releaseTasks();
    this.openLists.remove(list.id);
    this.finishedLists.addFirst(list);
    this.dropExpiredLists();
  }

  /** Forgets the lists over the number or the age to be kept; the oldest are last. */
  private void dropExpiredLists() {
    int kept = Math.max(0, this.retention.keptLists());
    Duration lifetime = this.retention.lifetime();
    Instant oldest = Instant.now().minus(lifetime);
    while (!this.finishedLists.isEmpty()) {
      ListState last = this.finishedLists.getLast();
      Instant finished = last.finished;
      boolean tooOld = finished != null && !finished.isAfter(oldest);
      if (this.finishedLists.size() > kept || tooOld) {
        this.finishedLists.removeLast();
      } else {
        break;
      }
    }
  }

  /** Drops the waiting tasks of the list, unless another list wants them too. */
  private void cancel(ListState list) {
    for (Entry entry : new ArrayList<>(this.waiting.values())) {
      if (entry.lists.contains(list)) {
        this.withdraw(entry, list);
      }
    }
    if (list.open == 0) {
      this.finish(list);
    }
  }

  private void withdraw(Entry entry, ListState list) {
    this.unindex(entry);
    entry.lists.remove(list);
    list.open--;
    list.cancelled++;
    if (entry.lists.isEmpty()) {
      this.waiting.remove(SlotKey.of(entry.task));
    } else {
      entry.priority = entry.mostUrgent();
      this.index(entry);
    }
  }

  /** The work is over for good: tells every list that asked for it. */
  private void settle(Hold hold, boolean succeeded) {
    for (ListState list : hold.lists) {
      list.open--;
      if (succeeded) {
        list.done++;
      } else {
        list.failed.add(hold.task);
      }
      if (list.open == 0) {
        this.finish(list);
      }
    }
  }

  /**
   * Requests a task again after an attempt that failed for a passing reason, unless a newer request
   * for its entity is waiting already. The lists that asked for the task wait for what follows it.
   */
  private void tryAgain(Hold hold, PublishTask task) {
    Entry next = this.waiting.get(SlotKey.of(task));
    if (next == null) {
      next = this.entryFor(task.nextAttempt());
      this.drainedAnnounced = false;
    } else {
      this.unindex(next);
    }
    for (ListState list : hold.lists) {
      if (next.lists.contains(list)) {
        // the list asked twice and is served once
        list.open--;
        list.total--;
      } else {
        next.lists.add(list);
        next.after.addAll(list.after);
      }
    }
    next.priority = next.mostUrgent();
    this.index(next);
  }

  // --- tasks, internally

  /** The waiting task for the entity and place, merged with the request, not indexed. */
  private Entry entryFor(PublishTask task) {
    SlotKey slot = SlotKey.of(task);
    Entry entry = this.waiting.get(slot);
    if (entry == null) {
      this.sequence++;
      entry = new Entry(task, this.sequence);
      this.waiting.put(slot, entry);
    } else {
      this.unindex(entry);
      entry.task = merge(entry.task, task);
    }
    return entry;
  }

  /** Makes the list wait for the task, once: tasks merged into one count as one. */
  private void attach(Entry entry, ListState list) {
    if (entry.lists.add(list)) {
      list.open++;
      list.total++;
      entry.after.addAll(list.after);
    }
    entry.priority = entry.mostUrgent();
  }

  /** One task for two requests: the younger says what to do, neither loses what it asked for. */
  private static PublishTask merge(PublishTask older, PublishTask younger) {
    Object attachment = younger.attachment() != null ? younger.attachment() : older.attachment();
    return new PublishTask(
        younger.key(),
        younger.type(),
        older.options().merge(younger.options()),
        Math.min(older.attempt(), younger.attempt()),
        attachment);
  }

  private void index(Entry entry) {
    if (entry.task.type() == PublishTaskType.CLEANUP) {
      this.cleanups.add(entry);
    } else {
      this.queue.add(entry);
    }
  }

  private void unindex(Entry entry) {
    if (entry.task.type() == PublishTaskType.CLEANUP) {
      this.cleanups.remove(entry);
    } else {
      this.queue.remove(entry);
    }
  }

  private void remove(Entry entry) {
    this.unindex(entry);
    this.waiting.remove(SlotKey.of(entry.task));
  }

  private @Nullable Entry findRunnable(PublishingLimits limits) {
    int allowed =
        this.foregroundActive > 0
            ? limits.parallelWhileInteractive()
            : Math.max(1, limits.parallel());
    if (this.cleaningUp || this.runningBackground >= allowed) {
      return null;
    }
    for (Entry entry : this.queue) {
      if (this.isFree(entry) && !entry.hasToWait()) {
        return entry;
      }
    }
    if (!this.holds.isEmpty()) {
      return null;
    }
    if (this.queue.isEmpty()) {
      return this.cleanups.isEmpty() ? null : this.cleanups.first();
    }
    return this.oldestWaitingInACircle();
  }

  private boolean isFree(Entry entry) {
    PublishTaskKey key = entry.task.key();
    return !this.holds.containsKey(key) && !this.foregroundWaiting.containsKey(key);
  }

  /**
   * Nothing is being published and nothing may start: the waiting tasks wait for lists that in turn
   * wait for them, which merging can cause. The first of them runs, so the rest can follow. A task
   * waiting for a foreground list is not in a circle: that list is published by its own thread.
   */
  private @Nullable Entry oldestWaitingInACircle() {
    for (Entry entry : this.queue) {
      if (this.isFree(entry) && !entry.waitsForForeground()) {
        return entry;
      }
    }
    return null;
  }

  private void start(Entry entry) {
    this.remove(entry);
    Hold hold = Hold.ofBackground(entry);
    entry.lists.forEach(hold::add);
    this.holds.put(entry.task.key(), hold);
    this.runningBackground++;
    if (entry.task.type() == PublishTaskType.CLEANUP) {
      this.cleaningUp = true;
    }
  }

  private void stopWaitingInForeground(PublishTaskKey key) {
    int remaining = this.foregroundWaiting.getOrDefault(key, 0) - 1;
    if (remaining > 0) {
      this.foregroundWaiting.put(key, remaining);
    } else {
      this.foregroundWaiting.remove(key);
    }
  }

  /** Publishing is suspended for a thread unless it suspended it or is in the middle of a task. */
  private boolean isSuspendedFor(Thread thread) {
    if (this.suspenders.isEmpty() || this.suspenders.contains(thread)) {
      return false;
    }
    return this.holds.values().stream().noneMatch(hold -> thread.equals(hold.owner));
  }

  private boolean isPublishedByOthers(Thread thread) {
    return this.holds.values().stream().anyMatch(hold -> !thread.equals(hold.owner));
  }

  /** A waiting task: mutable, since requests are merged into it. */
  private static final class Entry {

    private PublishTask task;

    /** When the task was first requested; its place among tasks of the same priority. */
    private final long sequence;

    /** As urgent as the most urgent list that asked for the task. */
    private PublishPriority priority = PublishPriority.BULK;

    /** The lists that asked for the task. */
    private final Set<ListState> lists = new HashSet<>();

    /** The lists whose other tasks have to be done before this one starts. */
    private final Set<ListState> after = new HashSet<>();

    private Entry(PublishTask task, long sequence) {
      this.task = task;
      this.sequence = sequence;
    }

    private PublishPriority mostUrgent() {
      PublishPriority most = PublishPriority.BULK;
      for (ListState list : this.lists) {
        most = most.higher(list.priority);
      }
      return most;
    }

    /** Whether a list this task comes after still has other tasks waiting or running. */
    private boolean hasToWait() {
      this.after.removeIf(list -> list.finished != null);
      for (ListState predecessor : this.after) {
        int own = this.lists.contains(predecessor) ? 1 : 0;
        if (predecessor.open - own > 0) {
          return true;
        }
      }
      return false;
    }

    private boolean waitsForForeground() {
      return this.after.stream().anyMatch(list -> list.foreground && list.finished == null);
    }
  }

  /** An entity being published. */
  private static final class Hold {

    private final boolean foreground;

    private final PublishTask task;

    /** The lists waiting for the publication to end. */
    private final Set<ListState> lists = new HashSet<>();

    /** The publishing thread; unknown for a background task until its worker adopts it. */
    private @Nullable Thread owner;

    private int depth = OUTERMOST;

    private Hold(boolean foreground, PublishTask task, @Nullable Thread owner) {
      this.foreground = foreground;
      this.task = task;
      this.owner = owner;
    }

    private static Hold ofBackground(Entry entry) {
      return new Hold(false, entry.task, null);
    }

    private static Hold ofForeground(Thread owner, PublishTask task) {
      return new Hold(true, task, owner);
    }

    /** The list waits for this publication from now on. */
    private void add(@Nullable ListState list) {
      if (list != null && this.lists.add(list)) {
        list.running++;
        if (list.started == null) {
          list.started = Instant.now();
        }
      }
    }
  }

  /** A list as the schedule keeps it: what was asked for and the counts of its tasks. */
  private static final class ListState {

    private final PublishListId id;

    /** What was asked for; once the list is over, without the tasks, which are of no use then. */
    private PublishListRequest request;

    private final PublishPriority priority;

    /** Whether the tasks are published by the thread that requested the list. */
    private final boolean foreground;

    /** The lists whose tasks have to be done first, as far as they were still going on. */
    private final Set<ListState> after = new HashSet<>();

    private final Instant created = Instant.now();

    /** When the first task of the list started, or {@code null} while none has. */
    private @Nullable Instant started;

    private @Nullable Instant finished;

    private int total;

    /** The tasks waiting or running. */
    private int open;

    private int running;

    private int done;

    private int cancelled;

    private final List<PublishTask> failed = new ArrayList<>();

    private ListState(PublishListId id, PublishListRequest request, boolean foreground) {
      this.id = id;
      this.request = request;
      this.priority = request.priority();
      this.foreground = foreground;
    }

    /** Drops the tasks of the request, keeping what describes the list. */
    private void releaseTasks() {
      this.request =
          new PublishListRequest(
              this.request.name(),
              this.request.origin(),
              this.request.cause(),
              this.request.priority(),
              this.request.after(),
              List.of());
      this.after.clear();
    }

    private PublishList snapshot() {
      return new PublishList(
          this.id,
          this.request.name(),
          this.request.origin(),
          this.request.cause(),
          this.priority,
          this.request.after(),
          this.total,
          this.open - this.running,
          this.running,
          this.done,
          this.failed,
          this.cancelled,
          this.created,
          this.started,
          this.finished);
    }
  }

  /** The place a waiting task takes: one per entity, channel and kind of work. */
  private record SlotKey(PublishTaskKey key, PublishTaskType.Slot slot) {

    private static SlotKey of(PublishTask task) {
      return new SlotKey(task.key(), task.type().slot());
    }
  }
}
