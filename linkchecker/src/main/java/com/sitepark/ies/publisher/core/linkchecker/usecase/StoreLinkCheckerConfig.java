package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerConfigStore;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerScheduler;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import jakarta.inject.Inject;

/**
 * Use case that persists a new link checker configuration and applies the resulting side effects,
 * such as resetting results and rescheduling.
 */
public class StoreLinkCheckerConfig {

  private final AccessControl accessControl;

  private final LinkCheckerConfigStore linkCheckerConfigStore;

  private final LinkCheckerScheduler linkCheckerScheduler;

  private final PublishedExternalLinkRepository publishedExternalLinkRepository;

  /**
   * Creates the use case with its required dependencies.
   *
   * @param accessControl checks whether the current user may store the configuration
   * @param linkCheckerConfigStore loads and persists the link checker configuration
   * @param linkCheckerScheduler is notified when the configuration changes
   * @param publishedExternalLinkRepository resets stored results when the checker is disabled
   */
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

  /**
   * Stores the given configuration, resetting results when the checker becomes disabled and
   * notifying the scheduler about the change.
   *
   * @param config the link checker configuration to store
   */
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
