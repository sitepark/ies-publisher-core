package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList;
import com.sitepark.ies.publisher.core.publishing.domain.service.PublishSchedule;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import jakarta.inject.Inject;

/**
 * Requests a list of tasks to be carried out in the background.
 *
 * <p>Whatever is published in the background is requested as a list, be it the entities following
 * a change or a whole site. The list says how urgent it is and which lists it comes after; it gets
 * an id it can be watched, waited for and cancelled by.
 *
 * <p>An entity is requested as often as something changes that it depends on. A task for an entity
 * that already has one waiting is merged into it: the task keeps its place, belongs to both lists
 * and is as urgent as the more urgent one. A task for an entity being published right now waits and
 * runs afterwards.
 */
public class EnqueuePublishTasks {

  private final PublishSchedule schedule;

  /**
   * Creates the use case.
   *
   * @param schedule what is published when
   */
  @Inject
  public EnqueuePublishTasks(PublishSchedule schedule) {
    this.schedule = schedule;
  }

  /**
   * Requests the list, all at once: none of its tasks is started before all are known.
   *
   * @param request the list
   * @return the id of the list
   */
  public PublishListId enqueue(PublishListRequest request) {
    return this.schedule.open(request);
  }

  /**
   * Requests the list and waits until each of its tasks is carried out, for a caller that publishes
   * many entities and wants to know when it is done - a full generation started by hand.
   *
   * <p>The tasks run in the background like any others, as many at a time as the limits allow, and
   * in the order of their priority: more urgent lists requested meanwhile go first. What the tasks
   * trigger in turn is not waited for. Interrupting the caller does not take the tasks back.
   *
   * @param request the list
   * @return the list as it ended
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public PublishList enqueueAndAwait(PublishListRequest request) throws InterruptedException {
    return this.schedule.await(this.schedule.open(request));
  }
}
