package com.sitepark.ies.publisher.core.publishing.domain.entity;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListOrigin;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * A list of publish tasks as it stands: what was asked for, and how far it has come.
 *
 * <p>A task requested by several lists is carried out once and counts in each of them.
 *
 * @param id identifies the list; must not be {@code null}
 * @param name what the list is, for people watching it; must not be {@code null}
 * @param origin who asked; must not be {@code null}
 * @param cause what led to the list, or {@code null}
 * @param priority how urgent the list is; must not be {@code null}
 * @param after the lists this one waits for; must not be {@code null}
 * @param total the number of tasks requested
 * @param waiting the tasks not started yet
 * @param running the tasks being carried out
 * @param done the tasks that were carried out
 * @param failed the tasks that failed for good; must not be {@code null}
 * @param cancelled the tasks dropped before they ran, because the list was cancelled
 * @param created when the list was requested; must not be {@code null}
 * @param started when the first task of the list started, or {@code null} while none has
 * @param finished when the last task ended, or {@code null} while tasks are waiting or running
 */
public record PublishList(
    PublishListId id,
    String name,
    PublishListOrigin origin,
    @Nullable String cause,
    PublishPriority priority,
    Set<PublishListId> after,
    int total,
    int waiting,
    int running,
    int done,
    List<PublishTask> failed,
    int cancelled,
    Instant created,
    @Nullable Instant started,
    @Nullable Instant finished) {

  /** Keeps the collections unmodifiable. */
  public PublishList {
    after = Set.copyOf(after);
    failed = List.copyOf(failed);
  }

  /**
   * Returns how long the list took, or has taken so far: from the start of its first task to its
   * end, or to now while it goes on.
   *
   * @return the time, or {@code null} if no task has started yet
   */
  public @Nullable Duration duration() {
    if (this.started == null) {
      return null;
    }
    return Duration.between(this.started, this.finished == null ? Instant.now() : this.finished);
  }

  /**
   * Returns whether no task of the list is waiting or running any more.
   *
   * @return {@code true} once the list is over
   */
  public boolean isFinished() {
    return this.finished != null;
  }

  /**
   * Returns whether every task of the list was carried out.
   *
   * @return {@code true} if the list is over and no task failed or was dropped
   */
  public boolean isComplete() {
    return this.isFinished() && this.failed.isEmpty() && this.cancelled == 0;
  }
}
