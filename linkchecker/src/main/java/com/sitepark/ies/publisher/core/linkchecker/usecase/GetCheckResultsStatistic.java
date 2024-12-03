package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultStatistic;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import jakarta.inject.Inject;

public class GetCheckResultsStatistic {
  private final AccessControl accessControl;
  private final PublishedExternalLinkRepository publishedExternalLinkRepository;

  @Inject
  public GetCheckResultsStatistic(
      AccessControl accessControl,
      PublishedExternalLinkRepository publishedExternalLinkRepository) {
    this.accessControl = accessControl;
    this.publishedExternalLinkRepository = publishedExternalLinkRepository;
  }

  public LinkCheckerResultStatistic getCheckResultsStatistic() {

    if (!this.accessControl.isAllowGetCheckResults()) {
      throw new AccessDeniedException("Not allowed to get check results statistic");
    }

    return this.publishedExternalLinkRepository.getCheckResultsStatistic();
  }
}
