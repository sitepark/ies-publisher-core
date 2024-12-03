package com.sitepark.ies.publisher.core.linkchecker.port;

public interface AccessControl {
  boolean isAllowRunLinkChecker();

  boolean isAllowStoreLinkCheckerConfig();

  boolean isAllowGetLinkCheckerConfig();

  boolean isAllowGetCheckResults();
}
