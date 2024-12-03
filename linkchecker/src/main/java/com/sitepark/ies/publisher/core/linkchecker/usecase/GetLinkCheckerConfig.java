package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerConfigStore;
import jakarta.inject.Inject;

public class GetLinkCheckerConfig {

  private final AccessControl accessControl;

  private final LinkCheckerConfigStore linkCheckerConfigStore;

  @Inject
  public GetLinkCheckerConfig(
      AccessControl accessControl, LinkCheckerConfigStore linkCheckerConfigStore) {
    this.linkCheckerConfigStore = linkCheckerConfigStore;
    this.accessControl = accessControl;
  }

  public LinkCheckerConfig get() {
    if (!this.accessControl.isAllowGetLinkCheckerConfig()) {
      throw new AccessDeniedException("Not allowed to get check results");
    }

    return this.linkCheckerConfigStore.get();
  }
}
