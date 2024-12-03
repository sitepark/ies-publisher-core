package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerConfigStore;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerScheduler;
import jakarta.inject.Inject;

public class StoreLinkCheckerConfig {

  private final AccessControl accessControl;

  private final LinkCheckerConfigStore linkCheckerConfigStore;

  private final LinkCheckerScheduler linkCheckerScheduler;

  @Inject
  public StoreLinkCheckerConfig(
      AccessControl accessControl,
      LinkCheckerConfigStore linkCheckerConfigStore,
      LinkCheckerScheduler linkCheckerScheduler) {
    this.accessControl = accessControl;
    this.linkCheckerConfigStore = linkCheckerConfigStore;
    this.linkCheckerScheduler = linkCheckerScheduler;
  }

  public void store(LinkCheckerConfig config) {

    if (!this.accessControl.isAllowGetLinkCheckerConfig()) {
      throw new AccessDeniedException("Not allowed to store link checker config");
    }

    this.linkCheckerConfigStore.store(config);
    this.linkCheckerScheduler.configChanged();
  }
}
