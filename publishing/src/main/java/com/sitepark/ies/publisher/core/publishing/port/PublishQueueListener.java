package com.sitepark.ies.publisher.core.publishing.port;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;

/** Is told what happens to background publishing. */
public interface PublishQueueListener {

  /**
   * Everything requested is done: nothing is waiting, nothing runs in the background. Called on the
   * dispatching thread, so no background task starts before it returns.
   */
  void drained();

  /**
   * The execution of a task ended with an error nobody handled.
   *
   * @param task the task
   * @param cause the error
   */
  void failed(PublishTask task, RuntimeException cause);
}
