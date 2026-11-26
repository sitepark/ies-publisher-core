package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.service.PublishSchedule;
import jakarta.inject.Inject;

/**
 * Keeps publishing out of the way while something changes that every publication relies on - the
 * templates of a module being replaced, for example.
 */
public class SuspendPublishing {

  private final PublishSchedule schedule;

  /**
   * Creates the use case.
   *
   * @param schedule what is published when
   */
  @Inject
  public SuspendPublishing(PublishSchedule schedule) {
    this.schedule = schedule;
  }

  /**
   * Waits until nothing is being published and lets nothing start, background or foreground, until
   * {@link #resume()} is called by the same thread.
   */
  public void suspend() {
    this.schedule.suspend();
  }

  /** Lets publishing go on. */
  public void resume() {
    this.schedule.resume();
  }
}
