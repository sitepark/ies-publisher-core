package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLink;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultItem;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.StatusType;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.LinkCheckerException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkChecker;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerConfigStore;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import com.sitepark.ies.publisher.core.linkchecker.service.LinkCheckerExcludesPatternMatcher;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LinkCheckByHashes {

  private final AccessControl accessControl;
  private final LinkChecker linkChecker;
  private final PublishedExternalLinkRepository publishedExternalLinkRepository;
  private final LinkCheckerConfigStore linkCheckerConfigStore;

  @Inject
  public LinkCheckByHashes(
      AccessControl accessControl,
      LinkChecker linkChecker,
      PublishedExternalLinkRepository publishedExternalLinkRepository,
      LinkCheckerConfigStore linkCheckerConfigStore) {
    this.accessControl = accessControl;
    this.linkChecker = linkChecker;
    this.publishedExternalLinkRepository = publishedExternalLinkRepository;
    this.linkCheckerConfigStore = linkCheckerConfigStore;
  }

  /** Check the links whose hashes have been specified */
  public List<LinkCheckerResultItem> linkCheckByHashes(Collection<String> hashes) {

    if (!this.accessControl.isAllowRunLinkChecker()) {
      throw new AccessDeniedException("Not allowed to run link checker");
    }

    LinkCheckerConfig config = this.linkCheckerConfigStore.get();

    LinkCheckerExcludesPatternMatcher linkCheckerExcludesPatternMatcher =
        new LinkCheckerExcludesPatternMatcher(config.getExcludes());

    List<LinkCheckerLink> linksToCheck = this.publishedExternalLinkRepository.getLinks(hashes);

    ExecutorService executorService = Executors.newFixedThreadPool(config.getParallel());
    List<LinkCheckerResultItem> resultList = Collections.synchronizedList(new ArrayList<>());
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (LinkCheckerLink link : linksToCheck) {
      CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
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
                resultList.add(
                    result.toBuilder().url(link.getUrl()).hash(linkWithTimeout.getHash()).build());
              },
              executorService);
      futures.add(future);
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
        throw new LinkCheckerException("Executor did not terminate in the specified time.");
      }
    } catch (InterruptedException e) {
      throw new LinkCheckerException("Executor awaitTermination interupted.", e);
    }

    return resultList;
  }
}
