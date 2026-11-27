package com.sitepark.ies.publisher.core.linkchecker.usecase;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLink;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultItem;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.StatusType;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.LinkCheckerDisabledException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkChecker;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerConfigStore;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import com.sitepark.ies.publisher.core.linkchecker.service.LinkCheckerExcludesPatternMatcher;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Use case that synchronously checks a selected set of links identified by their hashes and returns
 * the individual check results.
 */
public class LinkCheckByHashes {

  private final AccessControl accessControl;
  private final LinkChecker linkChecker;
  private final PublishedExternalLinkRepository publishedExternalLinkRepository;
  private final LinkCheckerConfigStore linkCheckerConfigStore;

  /**
   * Creates the use case with its required dependencies.
   *
   * @param accessControl checks whether the current user may run the link checker
   * @param linkChecker performs the actual check of a single link
   * @param publishedExternalLinkRepository provides the links to check and stores their results
   * @param linkCheckerConfigStore provides the current link checker configuration
   */
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

  /**
   * Check the links whose hashes have been specified.
   *
   * <p>Links excluded by the configuration are marked as ignored but not returned. A link whose
   * check fails with an error is returned with the status {@link StatusType#OTHER} and the error
   * message, so the results of the other links are not lost.
   *
   * @param hashes the hashes identifying the links to check
   * @return the check results for the checked links, in the order of the links
   */
  public List<LinkCheckerResultItem> linkCheckByHashes(Collection<String> hashes) {

    checkAccess();

    LinkCheckerConfig config = getConfig();

    LinkCheckerExcludesPatternMatcher linkCheckerExcludesPatternMatcher =
        new LinkCheckerExcludesPatternMatcher(config.getExcludes());

    List<LinkCheckerLink> linksToCheck = this.publishedExternalLinkRepository.getLinks(hashes);

    // closing the executor waits for the submitted tasks
    try (ExecutorService executorService = Executors.newFixedThreadPool(config.getParallel())) {

      List<CompletableFuture<Optional<LinkCheckerResultItem>>> futures = new ArrayList<>();
      for (LinkCheckerLink link : linksToCheck) {
        futures.add(
            CompletableFuture.supplyAsync(
                () -> this.check(link, linkCheckerExcludesPatternMatcher, config),
                executorService));
      }

      List<LinkCheckerResultItem> resultList = new ArrayList<>();
      for (CompletableFuture<Optional<LinkCheckerResultItem>> future : futures) {
        future.join().ifPresent(resultList::add);
      }
      return resultList;
    }
  }

  private void checkAccess() {
    if (!this.accessControl.isAllowRunLinkChecker()) {
      throw new AccessDeniedException("Not allowed to run link checker");
    }
  }

  private LinkCheckerConfig getConfig() {
    LinkCheckerConfig config = this.linkCheckerConfigStore.get();
    if (!config.isEnabled()) {
      throw new LinkCheckerDisabledException("Link checker is disabled");
    }
    return config;
  }

  // one failing link must not take down the results of the others
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private Optional<LinkCheckerResultItem> check(
      LinkCheckerLink link,
      LinkCheckerExcludesPatternMatcher linkCheckerExcludesPatternMatcher,
      LinkCheckerConfig config) {
    if (linkCheckerExcludesPatternMatcher.isExcluded(link.getUrl())) {
      this.publishedExternalLinkRepository.updateCheckResult(
          link.getHash(), LinkCheckerResultItem.builder().status(StatusType.IGNORED).build());
      return Optional.empty();
    }
    LinkCheckerLink linkWithTimeout = link.toBuilder().timeout(config.getTimeout()).build();
    LinkCheckerResultItem result;
    try {
      result = this.linkChecker.checkLink(linkWithTimeout);
    } catch (RuntimeException e) {
      // a failed check is not stored: the link stays unchecked
      return Optional.of(
          LinkCheckerResultItem.builder()
              .url(link.getUrl())
              .hash(link.getHash())
              .status(StatusType.OTHER)
              .message(String.valueOf(e.getMessage()))
              .build());
    }
    this.publishedExternalLinkRepository.updateCheckResult(linkWithTimeout.getHash(), result);
    return Optional.of(result.toBuilder().url(link.getUrl()).hash(link.getHash()).build());
  }
}
