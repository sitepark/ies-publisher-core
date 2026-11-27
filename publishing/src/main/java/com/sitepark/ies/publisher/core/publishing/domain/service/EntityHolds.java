package com.sitepark.ies.publisher.core.publishing.domain.service;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishingCapacity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Who publishes what in a {@link PublishSchedule}: the entities being published and by which
 * thread, the foreground publications waiting for an entity, and the threads that suspended
 * publishing. Counts what runs in the background and in the foreground, which decides how much more
 * may start.
 *
 * <p>Not thread-safe: only used under the lock of the schedule.
 */
// guarded by the lock of the schedule
@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.LawOfDemeter"})
final class EntityHolds {

  /** The entities being published. */
  private final Map<PublishTaskKey, Hold> holds = new HashMap<>();

  /** The foreground publications waiting for an entity, by entity. */
  private final Map<PublishTaskKey, Integer> foregroundWaiting = new HashMap<>();

  /** The threads that suspended publishing and have not resumed it. */
  private final List<Thread> suspenders = new ArrayList<>();

  private int runningBackground;

  /** The foreground publications going on or waiting for their entity. */
  private int foregroundActive;

  /** Whether a cleanup runs; nothing else is started in the background meanwhile. */
  private boolean cleaningUp;

  /** The publication of the entity, or {@code null} if it is not being published. */
  @Nullable Hold get(PublishTaskKey key) {
    return this.holds.get(key);
  }

  /** The entity of the hold is being published from now on. */
  void put(Hold hold) {
    this.holds.put(hold.task.key(), hold);
    if (!hold.foreground) {
      this.runningBackground++;
      if (hold.task.type() == PublishTaskType.CLEANUP) {
        this.cleaningUp = true;
      }
    }
  }

  /** The entity of the hold is not being published any more. */
  void release(Hold hold) {
    this.holds.remove(hold.task.key());
    if (hold.foreground) {
      this.foregroundActive--;
    } else {
      this.runningBackground--;
      if (hold.task.type() == PublishTaskType.CLEANUP) {
        this.cleaningUp = false;
      }
    }
  }

  /**
   * Whether another task may start in the background: no cleanup runs, and fewer tasks run than
   * allowed - fewer while the foreground publishes.
   */
  boolean mayStartBackground(PublishingCapacity capacity) {
    int allowed =
        this.foregroundActive > 0 ? capacity.parallelWhileInteractive() : capacity.parallel();
    return !this.cleaningUp && this.runningBackground < allowed;
  }

  int runningInBackground() {
    return this.runningBackground;
  }

  int activeInForeground() {
    return this.foregroundActive;
  }

  boolean isHeld(PublishTaskKey key) {
    return this.holds.containsKey(key);
  }

  /** Whether nothing is being published. */
  boolean isEmpty() {
    return this.holds.isEmpty();
  }

  /** Whether the background may take the entity: nobody publishes it or waits for it. */
  boolean isFree(PublishTaskKey key) {
    return !this.holds.containsKey(key) && !this.foregroundWaiting.containsKey(key);
  }

  /** Whether the thread publishes an entity right now, as a worker or in the foreground. */
  boolean isPublishing(Thread thread) {
    return this.holds.values().stream().anyMatch(hold -> thread.equals(hold.owner));
  }

  /** Whether an entity is being published by a thread other than this one. */
  boolean isPublishedByOthers(Thread thread) {
    return this.holds.values().stream().anyMatch(hold -> !thread.equals(hold.owner));
  }

  /**
   * A foreground publication begins and waits for the entity; the background leaves it alone
   * meanwhile. It counts as active until its hold is released.
   */
  void startWaitingInForeground(PublishTaskKey key) {
    this.foregroundActive++;
    this.foregroundWaiting.merge(key, 1, Integer::sum);
  }

  void stopWaitingInForeground(PublishTaskKey key) {
    int remaining = this.foregroundWaiting.getOrDefault(key, 0) - 1;
    if (remaining > 0) {
      this.foregroundWaiting.put(key, remaining);
    } else {
      this.foregroundWaiting.remove(key);
    }
  }

  void suspend(Thread thread) {
    this.suspenders.add(thread);
  }

  /**
   * Lifts the suspension of the thread, or the oldest one if the thread did not suspend, so a
   * suspension is not kept for good by a thread that is gone.
   */
  void resume(Thread thread) {
    if (!this.suspenders.remove(thread) && !this.suspenders.isEmpty()) {
      this.suspenders.remove(0);
    }
  }

  boolean isSuspended() {
    return !this.suspenders.isEmpty();
  }

  /** Publishing is suspended for a thread unless it suspended it or is in the middle of a task. */
  boolean isSuspendedFor(Thread thread) {
    if (this.suspenders.isEmpty() || this.suspenders.contains(thread)) {
      return false;
    }
    return !this.isPublishing(thread);
  }
}
