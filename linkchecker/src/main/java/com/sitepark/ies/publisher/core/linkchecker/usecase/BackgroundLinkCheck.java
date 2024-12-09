package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerBackgroundExecution;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLink;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLinkFilter;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultItem;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.StatusType;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.LinkCheckerDisabledException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkChecker;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerBackgroundExecutor;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerConfigStore;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import com.sitepark.ies.publisher.core.linkchecker.service.LinkCheckerExcludesPatternMatcher;
import jakarta.inject.Inject;
import java.util.List;

public class BackgroundLinkCheck {

  private final AccessControl accessControl;
  private final LinkChecker linkChecker;
  private final LinkCheckerBackgroundExecutor linkCheckerBackgroundExecutor;
  private final PublishedExternalLinkRepository publishedExternalLinkRepository;
  private final LinkCheckerConfigStore linkCheckerConfigStore;

  @Inject
  public BackgroundLinkCheck(
      AccessControl accessControl,
      LinkChecker linkChecker,
      LinkCheckerBackgroundExecutor linkCheckerBackgroundExecutor,
      PublishedExternalLinkRepository publishedExternalLinkRepository,
      LinkCheckerConfigStore linkCheckerConfigStore) {
    this.accessControl = accessControl;
    this.linkChecker = linkChecker;
    this.linkCheckerBackgroundExecutor = linkCheckerBackgroundExecutor;
    this.publishedExternalLinkRepository = publishedExternalLinkRepository;
    this.linkCheckerConfigStore = linkCheckerConfigStore;
  }

  /**
   * Check the links with the help of a background operation
   *
   * @return BackgroundExecution ID that can be used to track the progress
   */
  public String backgroundLinkCheck() {
    return backgroundLinkCheck(LinkCheckerLinkFilter.builder().build());
  }

  /**
   * Check the links with the help of a background operation
   *
   * @return BackgroundExecution ID that can be used to track the progress
   */
  public String backgroundLinkCheck(LinkCheckerLinkFilter filter) {

    if (!this.accessControl.isAllowRunLinkChecker()) {
      throw new AccessDeniedException("Not allowed to run link checker");
    }

    LinkCheckerConfig config = this.linkCheckerConfigStore.get();

    if (!config.isEnabled()) {
      throw new LinkCheckerDisabledException("Link checker is disabled");
    }

    LinkCheckerExcludesPatternMatcher linkCheckerExcludesPatternMatcher =
        new LinkCheckerExcludesPatternMatcher(config.getExcludes());

    this.publishedExternalLinkRepository.cleanupUnusedLinks();

    List<LinkCheckerLink> linksToCheck = this.publishedExternalLinkRepository.getLinks(filter);

    LinkCheckerBackgroundExecution execution =
        LinkCheckerBackgroundExecution.builder()
            .parallel(config.getParallel())
            .topic("publisher", "link-checker")
            .links(linksToCheck)
            .operation(
                link -> {
                  if (linkCheckerExcludesPatternMatcher.isExcluded(link.getUrl())) {
                    this.publishedExternalLinkRepository.updateCheckResult(
                        link.getHash(),
                        LinkCheckerResultItem.builder().status(StatusType.IGNORED).build());
                    return;
                  }
                  LinkCheckerLink linkWithTimeout =
                      link.toBuilder().timeout(config.getTimeout()).build();
                  LinkCheckerResultItem result = this.linkChecker.checkLink(linkWithTimeout);
                  this.publishedExternalLinkRepository.updateCheckResult(
                      linkWithTimeout.getHash(), result);
                })
            .build();

    return this.linkCheckerBackgroundExecutor.execute(execution);
  }
}
