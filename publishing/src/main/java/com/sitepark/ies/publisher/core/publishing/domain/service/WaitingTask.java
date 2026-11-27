package com.sitepark.ies.publisher.core.publishing.domain.service;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A waiting task: mutable, since requests are merged into it. Only touched under the lock of {@link
 * PublishSchedule}.
 */
final class WaitingTask {

  PublishTask task;

  /** When the task was first requested; its place among tasks of the same priority. */
  final long sequence;

  /** As urgent as the most urgent list that asked for the task. */
  PublishPriority priority = PublishPriority.BULK;

  /** The lists that asked for the task. */
  final Set<ListState> lists = new HashSet<>();

  /** The lists whose other tasks have to be done before this one starts. */
  final Set<ListState> after = new HashSet<>();

  WaitingTask(PublishTask task, long sequence) {
    this.task = task;
    this.sequence = sequence;
  }

  /** Makes the task as urgent as the most urgent list that asked for it. */
  void reprioritize() {
    PublishPriority most = PublishPriority.BULK;
    for (ListState list : this.lists) {
      most = most.higher(list.priority);
    }
    this.priority = most;
  }

  /**
   * Whether a foreground doing the other task serves this one as well: it does the same work, with
   * everything this task asked for. Otherwise this task stays and runs afterwards - an extra run is
   * harmless, a lost option or a revoked entity published again is not.
   */
  boolean isCoveredBy(PublishTask foreground) {
    return foreground.type() != PublishTaskType.CLEANUP
        && foreground.type() == this.task.type()
        && foreground.options().merge(this.task.options()).equals(foreground.options())
        && (this.task.attachment() == null
            || Objects.equals(this.task.attachment(), foreground.attachment()));
  }

  /**
   * Whether a list this task comes after still has other tasks waiting or running. Cleanups of the
   * list do not count: they run last, once nothing else is waiting, so waiting for them would make
   * this task wait for itself.
   */
  boolean hasToWait(CleanupCounts cleanups) {
    this.after.removeIf(ListState::isFinished);
    for (ListState predecessor : this.after) {
      int own = this.lists.contains(predecessor) ? 1 : 0;
      int others = predecessor.openTasks() - own;
      if (others > 0 && others > cleanups.of(predecessor)) {
        return true;
      }
    }
    return false;
  }

  /** Whether the task comes after a foreground list still going on. */
  boolean waitsForForeground() {
    return this.after.stream().anyMatch(list -> list.foreground && !list.isFinished());
  }
}
