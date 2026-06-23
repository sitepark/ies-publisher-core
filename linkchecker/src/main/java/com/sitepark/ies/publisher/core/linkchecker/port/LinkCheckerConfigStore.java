package com.sitepark.ies.publisher.core.linkchecker.port;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;

/** Persistence contract for storing and retrieving the link checker configuration. */
public interface LinkCheckerConfigStore {
  /**
   * Persists the given link checker configuration.
   *
   * @param config the configuration to store
   */
  void store(LinkCheckerConfig config);

  /**
   * Returns the currently stored link checker configuration.
   *
   * @return the current configuration
   */
  LinkCheckerConfig get();
}
