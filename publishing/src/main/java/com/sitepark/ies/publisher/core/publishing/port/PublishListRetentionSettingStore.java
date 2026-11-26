package com.sitepark.ies.publisher.core.publishing.port;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRetentionSetting;

/** Keeps how many lists that are over an administrator wants to have kept, and for how long. */
public interface PublishListRetentionSettingStore {

  /**
   * Returns the setting. A server nobody configured keeps {@link
   * PublishListRetentionSetting#DEFAULT}.
   *
   * @return the setting
   */
  PublishListRetentionSetting get();

  /**
   * Stores the setting. It applies from now on, without a restart.
   *
   * @param setting the setting
   */
  void store(PublishListRetentionSetting setting);
}
