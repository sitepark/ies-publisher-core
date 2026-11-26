package com.sitepark.ies.publisher.core.publishing.port;

/** How much background publishing may run at the same time. The limits may change while running. */
public interface PublishingLimits {

  /**
   * The number of background tasks that may run at the same time.
   *
   * @return the limit, at least 1
   */
  int parallel();

  /**
   * The number of background tasks that may run at the same time while a publication runs in the
   * foreground, for a user waiting for it.
   *
   * @return the limit, 0 to start no background task meanwhile
   */
  int parallelWhileInteractive();
}
