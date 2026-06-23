package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultStatistic;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import jakarta.inject.Inject;

/** Use case that aggregates the link check results into summary statistics. */
public class GetCheckResultsStatistic {
  private final AccessControl accessControl;
  private final PublishedExternalLinkRepository publishedExternalLinkRepository;

  /**
   * Creates the use case with its required dependencies.
   *
   * @param accessControl checks whether the current user may read check results
   * @param publishedExternalLinkRepository provides the aggregated check result statistics
   */
  @Inject
  public GetCheckResultsStatistic(
      AccessControl accessControl,
      PublishedExternalLinkRepository publishedExternalLinkRepository) {
    this.accessControl = accessControl;
    this.publishedExternalLinkRepository = publishedExternalLinkRepository;
  }

  /**
   * Returns the aggregated statistics over all stored check results.
   *
   * @return the check result statistics
   */
  public LinkCheckerResultStatistic getCheckResultsStatistic() {

    if (!this.accessControl.isAllowGetCheckResults()) {
      throw new AccessDeniedException("Not allowed to get check results statistic");
    }

    return this.publishedExternalLinkRepository.getCheckResultsStatistic();
  }
}
