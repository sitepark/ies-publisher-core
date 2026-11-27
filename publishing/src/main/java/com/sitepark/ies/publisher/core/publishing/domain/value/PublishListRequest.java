package com.sitepark.ies.publisher.core.publishing.domain.value;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Asks for tasks to be carried out as one list: whatever is published is requested this way, be it
 * one entity an editor saves, the entities following a change, or a whole site.
 *
 * @param name what the list is, for people watching it; must not be {@code null} or blank
 * @param origin who asked; must not be {@code null}
 * @param cause what led to the list - the entity or event behind it - or {@code null}
 * @param priority how urgent the list is; must not be {@code null}
 * @param after the lists whose tasks have to be done before the tasks of this one start: the
 *     entities linking to a moved entity wait for the list that publishes the move; must not be
 *     {@code null}
 * @param tasks the tasks, in the order they are wanted; must not be {@code null}
 */
public record PublishListRequest(
    String name,
    PublishListOrigin origin,
    @Nullable String cause,
    PublishPriority priority,
    Set<PublishListId> after,
    List<PublishTask> tasks) {

  /**
   * Validates the request and keeps its collections unmodifiable.
   *
   * @throws NullPointerException if {@code name}, {@code origin}, {@code priority}, {@code after} or
   *     {@code tasks} is {@code null}
   * @throws IllegalArgumentException if {@code name} is blank
   */
  public PublishListRequest {
    Objects.requireNonNull(name, "Name must not be null or blank");
    if (name.isBlank()) {
      throw new IllegalArgumentException("Name must not be null or blank");
    }
    Objects.requireNonNull(origin, "Origin must not be null");
    Objects.requireNonNull(priority, "Priority must not be null");
    Objects.requireNonNull(after, "Predecessors must not be null");
    Objects.requireNonNull(tasks, "Tasks must not be null");
    after = Set.copyOf(after);
    tasks = List.copyOf(tasks);
  }

  /**
   * A request without cause and predecessors.
   *
   * @param name what the list is
   * @param origin who asked
   * @param priority how urgent the list is
   * @param tasks the tasks
   * @return the request
   */
  public static PublishListRequest of(
      String name, PublishListOrigin origin, PublishPriority priority, List<PublishTask> tasks) {
    return new PublishListRequest(name, origin, null, priority, Set.of(), tasks);
  }

  /**
   * Returns this request with the cause that led to it.
   *
   * @param cause the entity or event behind the list
   * @return the request
   */
  public PublishListRequest causedBy(@Nullable String cause) {
    return new PublishListRequest(
        this.name, this.origin, cause, this.priority, this.after, this.tasks);
  }

  /**
   * Returns this request waiting for other lists.
   *
   * @param predecessors the lists whose tasks have to be done first
   * @return the request
   */
  public PublishListRequest after(Set<PublishListId> predecessors) {
    return new PublishListRequest(
        this.name, this.origin, this.cause, this.priority, predecessors, this.tasks);
  }
}
