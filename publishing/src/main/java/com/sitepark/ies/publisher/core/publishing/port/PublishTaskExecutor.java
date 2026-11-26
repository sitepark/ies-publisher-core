package com.sitepark.ies.publisher.core.publishing.port;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;

/**
 * Carries out a publish task in the background.
 *
 * <p>The executor has to read the state of the entity when it is called, not earlier: the task may
 * have waited, and may stand for several requests.
 */
@FunctionalInterface
public interface PublishTaskExecutor {

  /**
   * Carries out the task. No other task for the same entity and channel runs meanwhile.
   *
   * @param task the task
   * @return how it ended
   */
  PublishOutcome execute(PublishTask task);
}
