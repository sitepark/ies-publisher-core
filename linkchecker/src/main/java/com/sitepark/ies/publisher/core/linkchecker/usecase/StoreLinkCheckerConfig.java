package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerConfigStore;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerScheduler;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import jakarta.inject.Inject;

public class StoreLinkCheckerConfig {

  private final AccessControl accessControl;

  private final LinkCheckerConfigStore linkCheckerConfigStore;

  private final LinkCheckerScheduler linkCheckerScheduler;

  private final PublishedExternalLinkRepository publishedExternalLinkRepository;

  @Inject
  public StoreLinkCheckerConfig(
      AccessControl accessControl,
      LinkCheckerConfigStore linkCheckerConfigStore,
      LinkCheckerScheduler linkCheckerScheduler,
      PublishedExternalLinkRepository publishedExternalLinkRepository) {
    this.accessControl = accessControl;
    this.linkCheckerConfigStore = linkCheckerConfigStore;
    this.linkCheckerScheduler = linkCheckerScheduler;
    this.publishedExternalLinkRepository = publishedExternalLinkRepository;
  }

  public void store(LinkCheckerConfig config) {

    if (!this.accessControl.isAllowGetLinkCheckerConfig()) {
      throw new AccessDeniedException("Not allowed to store link checker config");
    }

    LinkCheckerConfig oldConfig = this.linkCheckerConfigStore.get();

    this.linkCheckerConfigStore.store(config);
    if (!config.isEnabled() && oldConfig.isEnabled()) {
      this.publishedExternalLinkRepository.resetResults();
    }

    this.linkCheckerScheduler.configChanged();
  }
}
