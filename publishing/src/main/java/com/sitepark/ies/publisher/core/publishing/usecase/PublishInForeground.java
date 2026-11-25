package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.service.PublishSchedule;
import jakarta.inject.Inject;

/**
 * Publishes on the calling thread, for a user waiting for the result - an editor saving an article.
 *
 * <p>The foreground has precedence: it never queues behind background tasks, and fewer of them are
 * started while it runs. It waits only if its own entity is being published right now, for as long
 * as that publication takes. A task waiting for the same work is dropped, since the foreground does
 * it.
 */
public class PublishInForeground {

  private final PublishSchedule schedule;

  /**
   * Creates the use case.
   *
   * @param schedule what is published when
   */
  @Inject
  public PublishInForeground(PublishSchedule schedule) {
    this.schedule = schedule;
  }

  /**
   * Publishes as soon as the entity of the task is free.
   *
   * @param task what is done, for which entity
   * @param publication the publishing itself
   * @param <E> the error the publication may end with
   * @throws E if publishing fails
   */
  public <E extends Exception> void publish(PublishTask task, ForegroundPublication<E> publication)
      throws E {
    this.schedule.enterForeground(task);
    try {
      publication.publish();
    } finally {
      this.schedule.leaveForeground(task.key());
    }
  }
}
