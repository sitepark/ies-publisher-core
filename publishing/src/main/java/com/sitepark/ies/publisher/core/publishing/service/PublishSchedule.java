package com.sitepark.ies.publisher.core.publishing.service;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishBatchResult;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishQueueStatus;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.port.PublishingLimits;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;

/**
 * Decides what is published when: the tasks waiting, the entities being published, and the rules
 * that keep parallel publishing correct.
 *
 * <p>The schedule owns no thread. Whoever dispatches asks it for the next task and blocks until
 * one may run; whoever publishes in the foreground asks it for the entity and blocks until it is
 * free. All state is guarded by one lock that is never held while anything is published.
 *
 * <p>The rules:
 *
 * <ul>
 *   <li>An entity is published in a channel by one thread at a time, background or foreground.
 *   <li>Requests for an entity that already has a task waiting are merged into that task. It keeps
 *       its place, takes the higher priority and the stricter ordering.
 *   <li>A request for an entity being published waits and runs afterwards: the running publication
 *       may have read the state before the change.
 *   <li>A follow-up waits for every task requested before it that is at least as urgent.
 *   <li>A cleanup runs alone, once no other task is waiting and nothing is being published. No
 *       background task starts while it runs.
 *   <li>While a publication runs in the foreground, fewer background tasks are started.
 *   <li>A foreground publication takes over a waiting task of its entity, unless that task is a
 *       follow-up, which still waits for something the foreground cannot know yet.
 * </ul>
 *
 * <p>A thread publishing one entity may publish another one in the foreground. Two threads doing so
 * for each other's entity would wait forever; nothing in the publisher does.
 */
@Singleton
// The maps are guarded by the lock; entries and holds are private state of this class.
@SuppressWarnings({
  "PMD.TooManyMethods",
  "PMD.CyclomaticComplexity",
  "PMD.CouplingBetweenObjects",
  "PMD.UseConcurrentHashMap",
  "PMD.LawOfDemeter"
})
public class PublishSchedule {

  /** How often a task is tried again after an attempt that failed for a passing reason. */
  private static final int MAX_RETRIES = 1;

  /** How long a dispatcher waits at most before it looks at the limits again, in milliseconds. */
  private static final long RECHECK_MILLIS = 1000;

  /** The depth of a publication that is not nested in another one of the same entity. */
  private static final int OUTERMOST = 1;

  private static final Comparator<Entry> BY_SEQUENCE = Comparator.comparingLong(e -> e.sequence);

  private static final Comparator<Entry> BY_PRIORITY =
      Comparator.<Entry, PublishPriority>comparing(e -> e.task.priority())
          .thenComparing(BY_SEQUENCE);

  private final ReentrantLock lock = new ReentrantLock();

  private final Condition changed = this.lock.newCondition();

  /** The waiting tasks by entity and place. */
  private final Map<SlotKey, Entry> waiting = new HashMap<>();

  /** The waiting tasks except cleanups, in the order they are handed out. */
  private final NavigableSet<Entry> queue = new TreeSet<>(BY_PRIORITY);

  /** The waiting cleanups, oldest first. */
  private final NavigableSet<Entry> cleanups = new TreeSet<>(BY_SEQUENCE);

  /** The sequences of the waiting tasks follow-ups have to wait for, by priority. */
  private final Map<PublishPriority, NavigableSet<Long>> waitingAhead =
      new EnumMap<>(PublishPriority.class);

  /** The sequences of the running tasks follow-ups have to wait for, by priority. */
  private final Map<PublishPriority, NavigableSet<Long>> runningAhead =
      new EnumMap<>(PublishPriority.class);

  /** The entities being published. */
  private final Map<PublishTaskKey, Hold> holds = new HashMap<>();

  /** The foreground publications waiting for an entity, by entity. */
  private final Map<PublishTaskKey, Integer> foregroundWaiting = new HashMap<>();

  /** The threads that suspended publishing and have not resumed it. */
  private final List<Thread> suspenders = new ArrayList<>();

  private long sequence;

  private int runningBackground;

  /** Whether a cleanup runs; nothing else is started in the background meanwhile. */
  private boolean cleaningUp;

  private int foregroundActive;

  private long processed;

  private long failed;

  /** Whether the dispatcher was told that everything is done, since the last request. */
  private boolean drainedAnnounced = true;

  /** Creates an empty schedule. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  public PublishSchedule() {
    for (PublishPriority priority : PublishPriority.values()) {
      this.waitingAhead.put(priority, new TreeSet<>());
      this.runningAhead.put(priority, new TreeSet<>());
    }
  }

  /**
   * Requests tasks, all at once: none of them is handed out before all are known.
   *
   * @param tasks the tasks, in the order they were requested
   */
  public void offer(Collection<PublishTask> tasks) {
    this.lock.lock();
    try {
      for (PublishTask task : tasks) {
        this.add(task);
      }
      if (!tasks.isEmpty()) {
        this.drainedAnnounced = false;
        this.changed.signalAll();
      }
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Requests tasks, all at once, and waits until each of them is carried out - whatever else is
   * requested meanwhile. The tasks are counted, not the queue watched: a task merged into one that
   * was waiting already is done when that one is, a task whose entity was being published is done
   * after the run that follows, and a task a foreground publication took over is done with it.
   *
   * <p>Interrupting the waiting thread does not take the tasks back.
   *
   * @param tasks the tasks, in the order they were requested
   * @return what became of the tasks
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public PublishBatchResult offerAndAwait(Collection<PublishTask> tasks)
      throws InterruptedException {
    Ticket ticket = new Ticket();
    this.lock.lockInterruptibly();
    try {
      for (PublishTask task : tasks) {
        attach(ticket, this.add(task));
      }
      if (!tasks.isEmpty()) {
        this.drainedAnnounced = false;
        this.changed.signalAll();
      }
      while (ticket.open > 0) {
        this.changed.await();
      }
      return new PublishBatchResult(ticket.done, ticket.failed, ticket.cancelled);
    } finally {
      this.lock.unlock();
    }
  }

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
      ahead(this.runningAhead, hold.priority).remove(hold.sequence);
      this.processed++;
      if (outcome == PublishOutcome.RETRY && task.attempt() < MAX_RETRIES) {
        this.tryAgain(hold, task);
      } else {
        if (outcome != PublishOutcome.DONE) {
          this.failed++;
        }
        settle(hold, outcome == PublishOutcome.DONE);
      }
      this.changed.signalAll();
    } finally {
      this.lock.unlock();
    }
  }

  /**
   * Waits until the entity of the task may be published in the foreground and marks it as being
   * published. A task waiting for the same work is dropped, since the foreground does it now;
   * a follow-up stays. Has to be paired with {@link #leaveForeground}.
   *
   * @param task what the foreground is about to do
   */
  public void enterForeground(PublishTask task) {
    PublishTaskKey key = task.key();
    Thread thread = Thread.currentThread();
    this.lock.lock();
    try {
      Hold own = this.holds.get(key);
      if (own != null && thread.equals(own.owner)) {
        own.depth++;
        return;
      }
      this.foregroundActive++;
      this.foregroundWaiting.merge(key, 1, Integer::sum);
      while (this.holds.containsKey(key) || this.isSuspendedFor(thread)) {
        this.changed.awaitUninterruptibly();
      }
      this.stopWaitingInForeground(key);
      Hold hold = Hold.ofForeground(thread, task);
      this.holds.put(key, hold);
      Entry covered = this.waiting.get(SlotKey.of(task));
      if (covered != null && covered.barrier == 0 && task.type() != PublishTaskType.CLEANUP) {
        this.remove(covered);
        // whoever waits for the task is served by the foreground
        hold.tickets.addAll(covered.tickets);
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
      settle(hold, succeeded);
      this.changed.signalAll();
    } finally {
      this.lock.unlock();
    }
  }

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

  /** Drops every waiting task. What is being published is finished. */
  public void clear() {
    this.lock.lock();
    try {
      for (Entry entry : this.waiting.values()) {
        for (Ticket ticket : entry.tickets) {
          ticket.open--;
          ticket.cancelled++;
        }
      }
      this.waiting.clear();
      this.queue.clear();
      this.cleanups.clear();
      this.waitingAhead.values().forEach(NavigableSet::clear);
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

  private void stopWaitingInForeground(PublishTaskKey key) {
    int remaining = this.foregroundWaiting.getOrDefault(key, 0) - 1;
    if (remaining > 0) {
      this.foregroundWaiting.put(key, remaining);
    } else {
      this.foregroundWaiting.remove(key);
    }
  }

  /**
   * Requests a task again after an attempt that failed for a passing reason, unless a newer request
   * for its entity is waiting already. Whoever waits for the task waits for what follows it.
   */
  private void tryAgain(Hold hold, PublishTask task) {
    Entry next = this.waiting.get(SlotKey.of(task));
    if (next == null) {
      next = this.add(task.nextAttempt());
      this.drainedAnnounced = false;
    }
    for (Ticket ticket : hold.tickets) {
      if (!next.tickets.add(ticket)) {
        ticket.open--;
      }
    }
  }

  /** Makes the ticket wait for the task, once: tasks merged into one count as one. */
  private static void attach(Ticket ticket, Entry entry) {
    if (entry.tickets.add(ticket)) {
      ticket.open++;
    }
  }

  /** The work is over for good: tells everybody waiting for it. */
  private static void settle(Hold hold, boolean succeeded) {
    for (Ticket ticket : hold.tickets) {
      ticket.open--;
      if (succeeded) {
        ticket.done++;
      } else {
        ticket.failed.add(hold.task);
      }
    }
  }

  private Entry add(PublishTask task) {
    this.sequence++;
    long requested = this.sequence;
    long barrier = task.followUp() ? requested : 0;
    SlotKey slot = SlotKey.of(task);
    Entry entry = this.waiting.get(slot);
    if (entry == null) {
      entry = new Entry(task, requested, barrier);
      this.waiting.put(slot, entry);
    } else {
      this.unindex(entry);
      entry.barrier = Math.max(entry.barrier, barrier);
      entry.task = merge(entry.task, task, entry.barrier > 0);
    }
    this.index(entry);
    return entry;
  }

  /** One task for two requests: the younger says what to do, neither loses what it asked for. */
  private static PublishTask merge(PublishTask older, PublishTask younger, boolean followUp) {
    Object attachment = younger.attachment() != null ? younger.attachment() : older.attachment();
    return new PublishTask(
        younger.key(),
        younger.type(),
        older.priority().higher(younger.priority()),
        followUp,
        older.options().merge(younger.options()),
        Math.min(older.attempt(), younger.attempt()),
        attachment);
  }

  private void index(Entry entry) {
    if (entry.task.type() == PublishTaskType.CLEANUP) {
      this.cleanups.add(entry);
      return;
    }
    this.queue.add(entry);
    if (entry.barrier == 0) {
      ahead(this.waitingAhead, entry.task.priority()).add(entry.sequence);
    }
  }

  private void unindex(Entry entry) {
    if (entry.task.type() == PublishTaskType.CLEANUP) {
      this.cleanups.remove(entry);
      return;
    }
    this.queue.remove(entry);
    ahead(this.waitingAhead, entry.task.priority()).remove(entry.sequence);
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
      if (this.mayRun(entry)) {
        return entry;
      }
    }
    if (this.queue.isEmpty() && this.holds.isEmpty() && !this.cleanups.isEmpty()) {
      return this.cleanups.first();
    }
    return null;
  }

  private boolean mayRun(Entry entry) {
    PublishTaskKey key = entry.task.key();
    if (this.holds.containsKey(key) || this.foregroundWaiting.containsKey(key)) {
      return false;
    }
    return entry.barrier == 0 || !this.hasToWait(entry);
  }

  /** A follow-up waits for the tasks requested before it that are at least as urgent. */
  private boolean hasToWait(Entry followUp) {
    for (PublishPriority priority : PublishPriority.values()) {
      if (!priority.isAtLeast(followUp.task.priority())) {
        continue;
      }
      if (isAhead(ahead(this.waitingAhead, priority), followUp.barrier)
          || isAhead(ahead(this.runningAhead, priority), followUp.barrier)) {
        return true;
      }
    }
    return false;
  }

  private static NavigableSet<Long> ahead(
      Map<PublishPriority, NavigableSet<Long>> sequences, PublishPriority priority) {
    return Objects.requireNonNull(sequences.get(priority), "every priority has its sequences");
  }

  private static boolean isAhead(NavigableSet<Long> sequences, long barrier) {
    return !sequences.isEmpty() && sequences.first() < barrier;
  }

  private void start(Entry entry) {
    this.remove(entry);
    this.holds.put(entry.task.key(), Hold.ofBackground(entry));
    this.runningBackground++;
    if (entry.task.type() == PublishTaskType.CLEANUP) {
      this.cleaningUp = true;
    } else if (entry.barrier == 0) {
      ahead(this.runningAhead, entry.task.priority()).add(entry.sequence);
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

    /** The sequence the task must not overtake, 0 if it waits for nothing. */
    private long barrier;

    /** Who waits for the task to be carried out. */
    private final Set<Ticket> tickets = new HashSet<>();

    private Entry(PublishTask task, long sequence, long barrier) {
      this.task = task;
      this.sequence = sequence;
      this.barrier = barrier;
    }
  }

  /** An entity being published. */
  private static final class Hold {

    private final boolean foreground;

    private final PublishTask task;

    /** Who waits for the publication to end. */
    private final Set<Ticket> tickets = new HashSet<>();

    private final PublishPriority priority;

    private final long sequence;

    /** The publishing thread; unknown for a background task until its worker adopts it. */
    private @Nullable Thread owner;

    private int depth = OUTERMOST;

    private Hold(
        boolean foreground,
        PublishTask task,
        PublishPriority priority,
        long sequence,
        @Nullable Thread owner) {
      this.foreground = foreground;
      this.task = task;
      this.priority = priority;
      this.sequence = sequence;
      this.owner = owner;
    }

    private static Hold ofBackground(Entry entry) {
      Hold hold = new Hold(false, entry.task, entry.task.priority(), entry.sequence, null);
      hold.tickets.addAll(entry.tickets);
      return hold;
    }

    private static Hold ofForeground(Thread owner, PublishTask task) {
      return new Hold(true, task, PublishPriority.DEPENDENCY, 0, owner);
    }
  }

  /** Tasks requested together by somebody waiting for all of them. */
  private static final class Ticket {

    /** The tasks not carried out yet. */
    private int open;

    private int done;

    private int cancelled;

    private final List<PublishTask> failed = new ArrayList<>();
  }

  /** The place a waiting task takes: one per entity, channel and kind of work. */
  private record SlotKey(PublishTaskKey key, PublishTaskType.Slot slot) {

    private static SlotKey of(PublishTask task) {
      return new SlotKey(task.key(), task.type().slot());
    }
  }
}
