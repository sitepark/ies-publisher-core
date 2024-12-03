package com.sitepark.ies.publisher.core.linkchecker.port;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLink;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLinkFilter;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResult;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultItem;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultStatistic;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.PublishedExternalLink;
import java.util.Collection;
import java.util.List;

public interface PublishedExternalLinkRepository {
  void store(String channel, String entity, List<PublishedExternalLink> links);

  List<LinkCheckerLink> getLinks();

  List<LinkCheckerLink> getLinks(Collection<String> hashes);

  LinkCheckerResult getCheckResult(LinkCheckerLinkFilter filter, int start, int limit);

  LinkCheckerResultStatistic getCheckResultsStatistic();

  void updateCheckResult(String hash, LinkCheckerResultItem result);

  void cleanupUnusedLinks();
}
