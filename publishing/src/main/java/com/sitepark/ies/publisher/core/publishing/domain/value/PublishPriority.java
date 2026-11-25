package com.sitepark.ies.publisher.core.publishing.domain.value;

/**
 * How urgent a list of publish tasks is. Tasks of a more urgent list are handed out first; a task
 * requested by several lists is as urgent as the most urgent of them.
 */
public enum PublishPriority {

  /**
   * Visitors or users wait for it: entities that have to follow a change made elsewhere, or
   * entities somebody asked for by name.
   */
  URGENT,

  /** Due, but nobody waits: entities whose time has come, published by a schedule. */
  NORMAL,

  /** Mass generation, a whole site for example. */
  BULK;

  /**
   * Returns whether tasks of this priority are handed out no later than tasks of the other.
   *
   * @param other the priority to compare with
   * @return {@code true} if this priority is the same or higher
   */
  public boolean isAtLeast(PublishPriority other) {
    return this.compareTo(other) <= 0;
  }

  /**
   * Returns the higher of two priorities.
   *
   * @param other the priority to compare with
   * @return this priority or the other, whichever is handed out first
   */
  public PublishPriority higher(PublishPriority other) {
    return this.isAtLeast(other) ? this : other;
  }
}
