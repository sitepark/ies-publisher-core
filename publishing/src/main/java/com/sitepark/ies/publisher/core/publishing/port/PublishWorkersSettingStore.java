package com.sitepark.ies.publisher.core.publishing.port;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishWorkersSetting;

/** Keeps how many workers an administrator wants to publish in the background. */
public interface PublishWorkersSettingStore {

  /**
   * Returns the setting. A server nobody configured yields {@link PublishWorkersSetting#auto()}.
   *
   * @return the setting
   */
  PublishWorkersSetting get();

  /**
   * Stores the setting. It applies from now on, without a restart.
   *
   * @param setting the setting
   */
  void store(PublishWorkersSetting setting);
}
