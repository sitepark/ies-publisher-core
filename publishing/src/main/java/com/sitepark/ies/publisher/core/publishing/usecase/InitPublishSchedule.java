package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.service.PublishSchedule;
import com.sitepark.ies.publisher.core.publishing.port.PublishListRetentionSettingStore;
import jakarta.inject.Inject;

/**
 * Prepares the schedule when the server starts: applies the settings an administrator stored, so
 * the schedule does not run on its defaults. To be called once at startup, before publishing
 * begins.
 *
 * <p>Runs on behalf of the server, not of a user, and therefore checks no rights.
 */
public class InitPublishSchedule {

  private final PublishListRetentionSettingStore retentionStore;

  private final PublishSchedule schedule;

  /**
   * Creates the use case.
   *
   * @param retentionStore keeps how many lists that are over are kept, and for how long
   * @param schedule the schedule to prepare
   */
  @Inject
  public InitPublishSchedule(
      PublishListRetentionSettingStore retentionStore, PublishSchedule schedule) {
    this.retentionStore = retentionStore;
    this.schedule = schedule;
  }

  /** Applies the stored settings to the schedule. */
  public void init() {
    this.schedule.retain(this.retentionStore.get());
  }
}
