package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishBatchResult;
import com.sitepark.ies.publisher.core.publishing.service.PublishSchedule;
import jakarta.inject.Inject;
import java.util.Collection;

/**
 * Requests tasks to be carried out in the background.
 *
 * <p>An entity is requested as often as something changes that it depends on. A request for an
 * entity that already has a task waiting is merged into that task: the task keeps its place, takes
 * the higher priority, and becomes a follow-up if either request is one. A request for an entity
 * being published right now waits and runs afterwards.
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
   * Requests the tasks, all at once: none of them is started before all are known.
   *
   * @param tasks the tasks, in the order they were requested
   */
  public void enqueue(Collection<PublishTask> tasks) {
    this.schedule.offer(tasks);
  }

  /**
   * Requests the tasks and waits until each of them is carried out, for a caller that publishes
   * many entities and wants to know when it is done - a full generation started by hand.
   *
   * <p>The tasks run in the background like any others, as many at a time as the limits allow, and
   * in the order of their priority: more urgent tasks requested meanwhile go first. What the tasks
   * trigger in turn is not waited for. Interrupting the caller does not take the tasks back.
   *
   * @param tasks the tasks, in the order they were requested
   * @return what became of the tasks
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public PublishBatchResult enqueueAndAwait(Collection<PublishTask> tasks)
      throws InterruptedException {
    return this.schedule.offerAndAwait(tasks);
  }
}
