package com.sitepark.ies.publisher.core.publishing.domain.value;

/** How the execution of a publish task ended. */
public enum PublishOutcome {

  /** The task is done, or turned out to be obsolete. */
  DONE,

  /**
   * The task failed for a reason that passes, a database deadlock for example. It is worth another
   * attempt.
   */
  RETRY,

  /** The task failed and another attempt would fail the same way. */
  FAILED
}
