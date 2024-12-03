package com.sitepark.ies.publisher.core.linkchecker.port;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLink;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultItem;

public interface LinkChecker {
  LinkCheckerResultItem checkLink(LinkCheckerLink link);
}
