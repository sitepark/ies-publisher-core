package com.sitepark.ies.publisher.core.publishing.port;

/**
 * The threads background publishing runs on.
 *
 * <p>The schedule never hands over more work at a time than {@link PublishingLimits#parallel()}
 * allows, so the workers need no queue of their own.
 */
@FunctionalInterface
public interface PublishWorkers {

  /**
   * Runs the work on a worker, without waiting for it.
   *
   * @param work the work
   */
  void submit(Runnable work);
}
