package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishQueueStatus;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.service.PublishSchedule;
import jakarta.inject.Inject;

/** Tells where background and foreground publishing stand. */
public class GetPublishQueueStatus {

  private final PublishSchedule schedule;

  /**
   * Creates the use case.
   *
   * @param schedule what is published when
   */
  @Inject
  public GetPublishQueueStatus(PublishSchedule schedule) {
    this.schedule = schedule;
  }

  /**
   * Returns where publishing stands.
   *
   * @return the status
   */
  public PublishQueueStatus get() {
    return this.schedule.status();
  }

  /**
   * Returns whether a task of the type is waiting for the entity.
   *
   * @param key the entity and channel
   * @param type what the task does
   * @return {@code true} if such a task is waiting
   */
  public boolean isWaiting(PublishTaskKey key, PublishTaskType type) {
    return this.schedule.isWaiting(key, type);
  }
}
