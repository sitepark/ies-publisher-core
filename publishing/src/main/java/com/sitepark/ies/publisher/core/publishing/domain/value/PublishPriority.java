package com.sitepark.ies.publisher.core.publishing.domain.value;

/** How urgent a publish task is. Tasks of a higher priority are handed out first. */
public enum PublishPriority {

  /**
   * The entity has to follow a change made elsewhere: a link target moved, a list it shows changed.
   * Visitors see outdated content until it is done.
   */
  DEPENDENCY,

  /** The entity is part of a mass generation, a whole site for example. */
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
