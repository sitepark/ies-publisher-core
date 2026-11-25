package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;

/**
 * What a foreground publication does for one of its tasks once the entity is free.
 *
 * @param <E> the error the publication may end with
 */
@FunctionalInterface
public interface ForegroundPublication<E extends Exception> {

  /**
   * Publishes. Has to read the state of the entity now, not earlier.
   *
   * @param task the task of the list being published
   * @throws E if publishing fails
   */
  void publish(PublishTask task) throws E;
}
