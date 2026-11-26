package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishWorkers;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishWorkersSetting;
import com.sitepark.ies.publisher.core.publishing.port.AccessControl;
import com.sitepark.ies.publisher.core.publishing.port.PublishWorkersSettingStore;
import com.sitepark.ies.publisher.core.publishing.port.PublishingResourcesProvider;
import jakarta.inject.Inject;

/**
 * Sets how many workers publish in the background: a number of the administrator's own, or left to
 * the server. The setting applies at once.
 */
public class SetPublishWorkers {

  private final AccessControl accessControl;

  private final PublishWorkersSettingStore store;

  private final PublishingResourcesProvider resources;

  /**
   * Creates the use case.
   *
   * @param accessControl checks whether the current user may manage publishing
   * @param store keeps the setting
   * @param resources tells what the server has
   */
  @Inject
  public SetPublishWorkers(
      AccessControl accessControl,
      PublishWorkersSettingStore store,
      PublishingResourcesProvider resources) {
    this.accessControl = accessControl;
    this.store = store;
    this.resources = resources;
  }

  /**
   * Stores the setting.
   *
   * @param setting the setting
   * @return the number of workers that publish from now on, with the limits behind it
   * @throws AccessDeniedException if the current user may not manage publishing
   */
  public PublishWorkers set(PublishWorkersSetting setting) {
    if (!this.accessControl.isAllowManagePublishing()) {
      throw new AccessDeniedException("Not allowed to set the publish workers");
    }
    this.store.store(setting);
    return PublishWorkers.calculate(setting, this.resources.get());
  }
}
