package com.sitepark.ies.publisher.core.publishing.domain.value;

/**
 * How much background publishing may run at the same time, as it stands right now.
 *
 * <p>The numbers come from wherever the limits are configured and are taken as they are: a number
 * of parallel tasks below 1 counts as 1, so background publishing never stops for good, and a
 * negative number while publishing in the foreground counts as 0.
 *
 * @param parallel the number of background tasks that may run at the same time, at least 1
 * @param parallelWhileInteractive the number of background tasks that may run at the same time
 *     while a publication runs in the foreground, for a user waiting for it; 0 to start no
 *     background task meanwhile
 */
public record PublishingCapacity(int parallel, int parallelWhileInteractive) {

  /** Raises the numbers below their minimum to it. */
  public PublishingCapacity {
    parallel = Math.max(1, parallel);
    parallelWhileInteractive = Math.max(0, parallelWhileInteractive);
  }
}
