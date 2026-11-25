package com.sitepark.ies.publisher.core.publishing.usecase;

/**
 * What a foreground publication does once its entity is free.
 *
 * @param <E> the error the publication may end with
 */
@FunctionalInterface
public interface ForegroundPublication<E extends Exception> {

  /**
   * Publishes. Has to read the state of the entity now, not earlier.
   *
   * @throws E if publishing fails
   */
  void publish() throws E;
}
