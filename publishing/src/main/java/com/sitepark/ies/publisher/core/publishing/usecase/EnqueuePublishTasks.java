package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
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
}
