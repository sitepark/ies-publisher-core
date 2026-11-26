package com.sitepark.ies.publisher.core.publishing.domain.entity;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskOptions;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import org.jspecify.annotations.Nullable;

/**
 * Something to do for an entity in a channel, sooner or later.
 *
 * <p>A task says that its work has to be done at least once after it was requested. Whoever
 * executes it reads the state of the entity only then, so one execution serves every request made
 * before it started. That is what allows merging requests for the same entity into one task.
 *
 * <p>How urgent a task is, and what it has to wait for, is said by the lists that request it.
 *
 * @param key the entity and channel; must not be {@code null}
 * @param type what to do; must not be {@code null}
 * @param options how to carry the task out; must not be {@code null}
 * @param attempt the number of attempts that failed before, 0 for a new task
 * @param attachment whatever the executor needs beyond this, handed through untouched, or {@code
 *     null}
 */
public record PublishTask(
    PublishTaskKey key,
    PublishTaskType type,
    PublishTaskOptions options,
    int attempt,
    @Nullable Object attachment) {

  /**
   * Validates that key, type and options are present.
   *
   * @throws IllegalArgumentException if {@code key}, {@code type} or {@code options} is {@code
   *     null}, or {@code attempt} is negative
   */
  public PublishTask {
    if (key == null) {
      throw new IllegalArgumentException("Key must not be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("Type must not be null");
    }
    if (options == null) {
      throw new IllegalArgumentException("Options must not be null");
    }
    if (attempt < 0) {
      throw new IllegalArgumentException("Attempt must not be negative");
    }
  }

  /**
   * A new task with default options.
   *
   * @param key the entity and channel
   * @param type what to do
   * @return the task
   */
  public static PublishTask of(PublishTaskKey key, PublishTaskType type) {
    return new PublishTask(key, type, PublishTaskOptions.DEFAULT, 0, null);
  }

  /**
   * Returns this task with other options.
   *
   * @param options how to carry the task out
   * @return the task
   */
  public PublishTask withOptions(PublishTaskOptions options) {
    return new PublishTask(this.key, this.type, options, this.attempt, this.attachment);
  }

  /**
   * Returns this task with an attachment for the executor.
   *
   * @param attachment whatever the executor needs beyond the task
   * @return the task
   */
  public PublishTask withAttachment(@Nullable Object attachment) {
    return new PublishTask(this.key, this.type, this.options, this.attempt, attachment);
  }

  /**
   * Returns this task as it is tried again after a failed attempt.
   *
   * @return the task with one more attempt counted
   */
  public PublishTask nextAttempt() {
    return new PublishTask(this.key, this.type, this.options, this.attempt + 1, this.attachment);
  }
}
