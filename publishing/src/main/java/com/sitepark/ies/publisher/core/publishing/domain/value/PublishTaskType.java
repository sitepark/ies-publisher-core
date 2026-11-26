package com.sitepark.ies.publisher.core.publishing.domain.value;

/** What a publish task does for an entity in a channel. */
public enum PublishTaskType {

  /** Generates the files of the entity. */
  PUBLISH(Slot.CONTENT),

  /** Revokes the files of the entity. */
  DEPUBLISH(Slot.CONTENT),

  /** Generates the translations of the entity. */
  TRANSLATION(Slot.TRANSLATION),

  /**
   * Deletes the files the entity no longer publishes. Runs only once nothing is waiting or running
   * that could still refer to them.
   */
  CLEANUP(Slot.CLEANUP);

  private final Slot waitingSlot;

  PublishTaskType(Slot slot) {
    this.waitingSlot = slot;
  }

  /**
   * The place a waiting task of this type takes for its entity. Tasks sharing a place are merged
   * into one: publishing and revoking share one, since the younger request supersedes the older.
   *
   * @return the place
   */
  public Slot slot() {
    return this.waitingSlot;
  }

  /** The places an entity can have a task waiting in. */
  public enum Slot {
    /** Publishing or revoking the entity. */
    CONTENT,
    /** Translating the entity. */
    TRANSLATION,
    /** Cleaning up after the entity. */
    CLEANUP
  }
}
