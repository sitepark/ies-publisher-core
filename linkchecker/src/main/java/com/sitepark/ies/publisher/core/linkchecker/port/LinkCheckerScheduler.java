package com.sitepark.ies.publisher.core.linkchecker.port;

/** Contract for the scheduler that triggers periodic link checks. */
public interface LinkCheckerScheduler {
  /** Notifies the scheduler that the link checker configuration has changed and must be reloaded. */
  void configChanged();
}
