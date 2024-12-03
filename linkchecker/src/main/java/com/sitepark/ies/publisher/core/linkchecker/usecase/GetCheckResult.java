package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLinkFilter;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResult;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import jakarta.inject.Inject;

public class GetCheckResult {
  private final AccessControl accessControl;
  private final PublishedExternalLinkRepository publishedExternalLinkRepository;

  @Inject
  public GetCheckResult(
      AccessControl accessControl,
      PublishedExternalLinkRepository publishedExternalLinkRepository) {
    this.accessControl = accessControl;
    this.publishedExternalLinkRepository = publishedExternalLinkRepository;
  }

  public LinkCheckerResult getCheckResult(LinkCheckerLinkFilter filter, int start, int limit) {

    if (!this.accessControl.isAllowGetCheckResults()) {
      throw new AccessDeniedException("Not allowed to get check results");
    }

    return this.publishedExternalLinkRepository.getCheckResult(filter, start, limit);
  }
}
