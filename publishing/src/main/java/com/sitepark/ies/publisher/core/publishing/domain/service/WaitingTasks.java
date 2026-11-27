package com.sitepark.ies.publisher.core.publishing.domain.service;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;

/**
 * The tasks of a {@link PublishSchedule} waiting to be published: one per entity, channel and kind
 * of work, ordered by priority and then by the time they were first requested. Cleanups are kept
 * apart, oldest first.
 *
 * <p>Not thread-safe: only used under the lock of the schedule. Keeps the counts of the lists
 * right as tasks are added, merged and withdrawn; when a list is over is for the schedule to tell.
 */
// guarded by the lock of the schedule
@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.LawOfDemeter", "PMD.TooManyMethods"})
final class WaitingTasks {

  private static final Comparator<WaitingTask> BY_SEQUENCE =
      Comparator.comparingLong(waiting -> waiting.sequence);

  private static final Comparator<WaitingTask> BY_PRIORITY =
      Comparator.<WaitingTask, PublishPriority>comparing(waiting -> waiting.priority)
          .thenComparing(BY_SEQUENCE);

  /** The waiting tasks by entity and place. */
  private final Map<SlotKey, WaitingTask> bySlot = new HashMap<>();

  /** The waiting tasks except cleanups, in the order they are handed out. */
  private final NavigableSet<WaitingTask> queue = new TreeSet<>(BY_PRIORITY);

  /** The waiting cleanups, oldest first. */
  private final NavigableSet<WaitingTask> cleanups = new TreeSet<>(BY_SEQUENCE);

  private long sequence;

  /**
   * Requests the task for the list: merged into the task waiting for the same entity and place, or
   * waiting on its own. A list asking twice for one task waits for it once.
   */
  void add(PublishTask task, ListState list) {
    WaitingTask waiting = this.request(task);
    if (waiting.lists.add(list)) {
      list.taskAdded();
      waiting.after.addAll(list.after);
    }
    waiting.reprioritize();
    this.index(waiting);
  }

  /** Drops the waiting tasks of the list, unless another list wants them too. */
  void withdraw(ListState list) {
    for (WaitingTask waiting : this.all()) {
      if (waiting.lists.contains(list)) {
        this.unindex(waiting);
        waiting.lists.remove(list);
        list.taskWithdrawn();
        if (waiting.lists.isEmpty()) {
          this.bySlot.remove(SlotKey.of(waiting.task));
        } else {
          waiting.reprioritize();
          this.index(waiting);
        }
      }
    }
  }

  /**
   * Requests the next attempt of a task for the lists that asked for it, unless a newer request
   * for its entity is waiting already; then the lists wait for that one.
   *
   * @return whether the task waits anew, rather than being served by one waiting already
   */
  boolean requeue(PublishTask task, Collection<ListState> lists) {
    WaitingTask next = this.get(task);
    boolean anew = next == null;
    if (next == null) {
      next = this.request(task.nextAttempt());
    } else {
      this.unindex(next);
    }
    for (ListState list : lists) {
      if (next.lists.add(list)) {
        next.after.addAll(list.after);
      } else {
        // the list asked twice and is served once
        list.taskMerged();
      }
    }
    next.reprioritize();
    this.index(next);
    return anew;
  }

  /**
   * Takes the waiting task that a foreground doing the task serves as well, if there is one and it
   * may run now: the foreground does it instead.
   *
   * @return the task taken over, or {@code null}
   */
  @Nullable WaitingTask takeOverBy(PublishTask foreground) {
    WaitingTask covered = this.get(foreground);
    if (covered == null
        || !covered.isCoveredBy(foreground)
        || covered.hasToWait(this.cleanupCounts())) {
      return null;
    }
    this.remove(covered);
    return covered;
  }

  /** The waiting task taking the place the task would take, or {@code null}. */
  @Nullable WaitingTask get(PublishTask task) {
    return this.bySlot.get(SlotKey.of(task));
  }

  /** Whether a task of the type is waiting for the entity. */
  boolean isWaiting(PublishTaskKey key, PublishTaskType type) {
    WaitingTask waiting = this.bySlot.get(new SlotKey(key, type.slot()));
    return waiting != null && waiting.task.type() == type;
  }

  /** Puts the task into the order it is handed out in. */
  private void index(WaitingTask waiting) {
    if (waiting.task.type() == PublishTaskType.CLEANUP) {
      this.cleanups.add(waiting);
    } else {
      this.queue.add(waiting);
    }
  }

  /** Takes the task out of the order it is handed out in; it is still waiting. */
  private void unindex(WaitingTask waiting) {
    if (waiting.task.type() == PublishTaskType.CLEANUP) {
      this.cleanups.remove(waiting);
    } else {
      this.queue.remove(waiting);
    }
  }

  /** The task waits no more. */
  void remove(WaitingTask waiting) {
    this.unindex(waiting);
    this.bySlot.remove(SlotKey.of(waiting.task));
  }

  /** The waiting tasks except cleanups, in the order they are handed out; not to be changed. */
  NavigableSet<WaitingTask> inOrder() {
    return Collections.unmodifiableNavigableSet(this.queue);
  }

  /** All waiting tasks, cleanups included; a copy. */
  List<WaitingTask> all() {
    return new ArrayList<>(this.bySlot.values());
  }

  /** Whether nothing but cleanups is waiting, or nothing at all. */
  boolean onlyCleanups() {
    return this.queue.isEmpty();
  }

  /** The cleanup waiting longest, or {@code null}. */
  @Nullable WaitingTask oldestCleanup() {
    return this.cleanups.isEmpty() ? null : this.cleanups.first();
  }

  /** The waiting cleanups per list, counted when first asked for: mostly nobody asks. */
  CleanupCounts cleanupCounts() {
    return new CleanupCounts(this.cleanups);
  }

  boolean isEmpty() {
    return this.bySlot.isEmpty();
  }

  int size() {
    return this.bySlot.size();
  }

  /**
   * The waiting task for the entity and place of the request, merged with it, or a new one. Not in
   * the order: put it there with {@link #index} once its lists and priority are set.
   */
  private WaitingTask request(PublishTask task) {
    SlotKey slot = SlotKey.of(task);
    WaitingTask waiting = this.bySlot.get(slot);
    if (waiting == null) {
      this.sequence++;
      waiting = new WaitingTask(task, this.sequence);
      this.bySlot.put(slot, waiting);
    } else {
      this.unindex(waiting);
      waiting.task = merge(waiting.task, task);
    }
    return waiting;
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
}
