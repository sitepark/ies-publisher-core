package com.sitepark.ies.publisher.core.linkchecker.port;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLink;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLinkFilter;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResult;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultItem;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultStatistic;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.PublishedExternalLink;
import java.util.Collection;
import java.util.List;

/** Persistence contract for published external links and their link check results. */
public interface PublishedExternalLinkRepository {
  /**
   * Stores the external links published by an entity within a channel.
   *
   * @param channel the channel the entity was published to
   * @param entity the identifier of the published entity
   * @param links the external links contained in the published entity
   */
  void store(String channel, String entity, List<PublishedExternalLink> links);

  /**
   * Returns all known links to be checked.
   *
   * @return all stored links
   */
  List<LinkCheckerLink> getLinks();

  /**
   * Returns the links identified by the given hashes.
   *
   * @param hashes the hashes of the links to retrieve
   * @return the matching links
   */
  List<LinkCheckerLink> getLinks(Collection<String> hashes);

  /**
   * Returns the links matching the given filter.
   *
   * @param filter the filter restricting the returned links
   * @return the matching links
   */
  List<LinkCheckerLink> getLinks(LinkCheckerLinkFilter filter);

  /**
   * Returns a paged check result for the links matching the given filter.
   *
   * @param filter the filter restricting the result
   * @param start the zero-based index of the first item to return
   * @param limit the maximum number of items to return
   * @return the paged check result
   */
  LinkCheckerResult getCheckResult(LinkCheckerLinkFilter filter, int start, int limit);

  /**
   * Returns aggregated statistics over all check results.
   *
   * @return the check result statistics
   */
  LinkCheckerResultStatistic getCheckResultsStatistic();

  /**
   * Updates the stored check result for the link with the given hash.
   *
   * @param hash the hash of the link whose result is updated
   * @param result the new check result
   */
  void updateCheckResult(String hash, LinkCheckerResultItem result);

  /** Removes links that are no longer referenced by any published entity. */
  void cleanupUnusedLinks();

  /** Resets all stored check results. */
  void resetResults();
}
