package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishWorkers;
import com.sitepark.ies.publisher.core.publishing.port.AccessControl;
import com.sitepark.ies.publisher.core.publishing.port.PublishWorkersSettingStore;
import com.sitepark.ies.publisher.core.publishing.port.PublishingResourcesProvider;
import jakarta.inject.Inject;

/**
 * Tells how many workers publish in the background and what that number comes from, for an
 * administrator deciding whether to leave it to the server or to set it.
 */
public class GetPublishWorkers {

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
  public GetPublishWorkers(
      AccessControl accessControl,
      PublishWorkersSettingStore store,
      PublishingResourcesProvider resources) {
    this.accessControl = accessControl;
    this.store = store;
    this.resources = resources;
  }

  /**
   * Returns the number of workers with the setting and the limits behind it.
   *
   * @return the workers
   * @throws AccessDeniedException if the current user may not manage publishing
   */
  public PublishWorkers get() {
    if (!this.accessControl.isAllowManagePublishing()) {
      throw new AccessDeniedException("Not allowed to get the publish workers");
    }
    return PublishWorkers.calculate(this.store.get(), this.resources.get());
  }
}
