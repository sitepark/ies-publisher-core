package com.sitepark.ies.publisher.core.publishing.domain.service;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList;
import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * A list as the schedule keeps it: what was asked for and the counts of its tasks. Mutable and not
 * thread-safe: only touched under the lock of {@link PublishSchedule}.
 */
// one method per thing that happens to a task of the list, so its counts stay consistent
@SuppressWarnings("PMD.TooManyMethods")
final class ListState {

  final PublishListId id;

  /** What was asked for; once the list is over, without the tasks, which are of no use then. */
  private PublishListRequest request;

  final PublishPriority priority;

  /** Whether the tasks are published by the thread that requested the list. */
  final boolean foreground;

  /** The lists whose tasks have to be done first, as far as they were still going on. */
  final Set<ListState> after = new HashSet<>();

  private final Instant created;

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

  ListState(PublishListId id, PublishListRequest request, boolean foreground, Instant created) {
    this.id = id;
    this.created = created;
    this.request = request;
    this.priority = request.priority();
    this.foreground = foreground;
  }

  /** A foreground list expects the tasks it was requested with; its thread hands them in. */
  void expectRequestedTasks() {
    this.total = this.request.tasks().size();
    this.open = this.total;
  }

  /** The list waits for one more task. */
  void taskAdded() {
    this.open++;
    this.total++;
  }

  /** A waiting task of the list was dropped. */
  void taskWithdrawn() {
    this.open--;
    this.cancelled++;
  }

  /** The list asked twice for a task that is done once. */
  void taskMerged() {
    this.open--;
    this.total--;
  }

  void taskStarted(Instant now) {
    this.running++;
    if (this.started == null) {
      this.started = now;
    }
  }

  /** A task stopped running; it may still be tried again. */
  void taskStopped() {
    this.running--;
  }

  /** A task is over for good. */
  void taskEnded(PublishTask task, boolean succeeded) {
    this.open--;
    if (succeeded) {
      this.done++;
    } else {
      this.failed.add(task);
    }
  }

  /** The tasks of a foreground list its thread never got to count as cancelled. */
  void cancelNeverStarted() {
    int neverStarted = this.open - this.running;
    this.open -= neverStarted;
    this.cancelled += neverStarted;
  }

  /** The tasks waiting or running. */
  int openTasks() {
    return this.open;
  }

  /** Whether no task of the list is waiting or running. */
  boolean isOver() {
    return this.open == 0;
  }

  boolean isFinished() {
    return this.finished != null;
  }

  @Nullable Instant finishedAt() {
    return this.finished;
  }

  /** The list is over: it keeps what describes it, not its tasks. */
  void finish(Instant now) {
    this.finished = now;
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

  PublishList snapshot() {
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
