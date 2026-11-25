package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.service.PublishSchedule;
import jakarta.inject.Inject;

/**
 * Publishes a list on the calling thread, for a user waiting for the result - an editor saving an
 * article.
 *
 * <p>The foreground has precedence: it never queues behind background tasks, and fewer of them are
 * started while it runs. It waits only if one of its own entities is being published right now, for
 * as long as that publication takes. A task waiting for the same work is taken over, since the
 * foreground does it.
 *
 * <p>The list can be watched like any other while it is published.
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
   * Publishes the tasks of the list one after the other, each as soon as its entity is free. The
   * first task that fails ends the list: the tasks after it are not started.
   *
   * @param request the list
   * @param publication the publishing of one task
   * @param <E> the error a publication may end with
   * @return the id of the list
   * @throws E if publishing a task fails
   */
  public <E extends Exception> PublishListId publish(
      PublishListRequest request, ForegroundPublication<E> publication) throws E {
    PublishListId list = this.schedule.openForeground(request);
    try {
      for (PublishTask task : request.tasks()) {
        this.publish(task, list, publication);
      }
    } finally {
      this.schedule.closeForeground(list);
    }
    return list;
  }

  private <E extends Exception> void publish(
      PublishTask task, PublishListId list, ForegroundPublication<E> publication) throws E {
    this.schedule.enterForeground(task, list);
    boolean succeeded = false;
    try {
      publication.publish(task);
      succeeded = true;
    } finally {
      this.schedule.leaveForeground(task.key(), succeeded);
    }
  }
}
