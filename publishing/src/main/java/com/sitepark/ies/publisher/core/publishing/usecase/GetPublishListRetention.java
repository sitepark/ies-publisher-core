package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRetentionSetting;
import com.sitepark.ies.publisher.core.publishing.port.AccessControl;
import com.sitepark.ies.publisher.core.publishing.port.PublishListRetentionSettingStore;
import jakarta.inject.Inject;

/** Tells how many lists that are over are kept, and for how long. */
public class GetPublishListRetention {

  private final AccessControl accessControl;

  private final PublishListRetentionSettingStore store;

  /**
   * Creates the use case.
   *
   * @param accessControl checks whether the current user may manage publishing
   * @param store keeps the setting
   */
  @Inject
  public GetPublishListRetention(
      AccessControl accessControl, PublishListRetentionSettingStore store) {
    this.accessControl = accessControl;
    this.store = store;
  }

  /**
   * Returns the setting.
   *
   * @return the setting
   * @throws AccessDeniedException if the current user may not manage publishing
   */
  public PublishListRetentionSetting get() {
    if (!this.accessControl.isAllowManagePublishing()) {
      throw new AccessDeniedException("Not allowed to get the retention of the publish lists");
    }
    return this.store.get();
  }
}
