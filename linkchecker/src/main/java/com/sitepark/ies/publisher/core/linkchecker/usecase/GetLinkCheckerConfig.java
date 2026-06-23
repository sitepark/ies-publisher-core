package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerConfigStore;
import jakarta.inject.Inject;

/** Use case that loads the current link checker configuration. */
public class GetLinkCheckerConfig {

  private final AccessControl accessControl;

  private final LinkCheckerConfigStore linkCheckerConfigStore;

  /**
   * Creates the use case with its required dependencies.
   *
   * @param accessControl checks whether the current user may read the configuration
   * @param linkCheckerConfigStore provides the stored link checker configuration
   */
  @Inject
  public GetLinkCheckerConfig(
      AccessControl accessControl, LinkCheckerConfigStore linkCheckerConfigStore) {
    this.linkCheckerConfigStore = linkCheckerConfigStore;
    this.accessControl = accessControl;
  }

  /**
   * Returns the current link checker configuration.
   *
   * @return the stored link checker configuration
   */
  public LinkCheckerConfig get() {
    if (!this.accessControl.isAllowGetLinkCheckerConfig()) {
      throw new AccessDeniedException("Not allowed to get check results");
    }

    return this.linkCheckerConfigStore.get();
  }
}
