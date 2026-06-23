package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLinkFilter;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResult;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import jakarta.inject.Inject;

/** Use case that retrieves a paginated set of link check results. */
public class GetCheckResult {
  private final AccessControl accessControl;
  private final PublishedExternalLinkRepository publishedExternalLinkRepository;

  /**
   * Creates the use case with its required dependencies.
   *
   * @param accessControl checks whether the current user may read check results
   * @param publishedExternalLinkRepository provides the stored check results
   */
  @Inject
  public GetCheckResult(
      AccessControl accessControl,
      PublishedExternalLinkRepository publishedExternalLinkRepository) {
    this.accessControl = accessControl;
    this.publishedExternalLinkRepository = publishedExternalLinkRepository;
  }

  /**
   * Returns the check results matching the given filter within the requested page window.
   *
   * @param filter restricts the results to be returned
   * @param start zero-based index of the first result to return
   * @param limit maximum number of results to return
   * @return the matching check results
   */
  public LinkCheckerResult getCheckResult(LinkCheckerLinkFilter filter, int start, int limit) {

    if (!this.accessControl.isAllowGetCheckResults()) {
      throw new AccessDeniedException("Not allowed to get check results");
    }

    return this.publishedExternalLinkRepository.getCheckResult(filter, start, limit);
  }
}
