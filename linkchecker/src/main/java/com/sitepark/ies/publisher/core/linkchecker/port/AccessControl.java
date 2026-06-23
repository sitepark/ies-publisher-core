package com.sitepark.ies.publisher.core.linkchecker.port;

/** Authorization contract for the link checker operations of the current user. */
public interface AccessControl {
  /**
   * Returns whether the current user may run the link checker.
   *
   * @return {@code true} if running the link checker is permitted
   */
  boolean isAllowRunLinkChecker();

  /**
   * Returns whether the current user may store the link checker configuration.
   *
   * @return {@code true} if storing the configuration is permitted
   */
  boolean isAllowStoreLinkCheckerConfig();

  /**
   * Returns whether the current user may read the link checker configuration.
   *
   * @return {@code true} if reading the configuration is permitted
   */
  boolean isAllowGetLinkCheckerConfig();

  /**
   * Returns whether the current user may read link check results.
   *
   * @return {@code true} if reading check results is permitted
   */
  boolean isAllowGetCheckResults();
}
