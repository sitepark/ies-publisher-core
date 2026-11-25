package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.service.PublishSchedule;
import jakarta.inject.Inject;

/** Drops the tasks waiting in the background, when publishing is stopped by an administrator. */
public class ClearPublishQueue {

  private final PublishSchedule schedule;

  /**
   * Creates the use case.
   *
   * @param schedule what is published when
   */
  @Inject
  public ClearPublishQueue(PublishSchedule schedule) {
    this.schedule = schedule;
  }

  /** Drops every waiting task. What is being published is finished. */
  public void clear() {
    this.schedule.clear();
  }
}
