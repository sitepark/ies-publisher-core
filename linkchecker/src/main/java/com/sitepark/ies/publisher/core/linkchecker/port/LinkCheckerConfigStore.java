package com.sitepark.ies.publisher.core.linkchecker.port;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;

public interface LinkCheckerConfigStore {
  void store(LinkCheckerConfig config);

  LinkCheckerConfig get();
}
