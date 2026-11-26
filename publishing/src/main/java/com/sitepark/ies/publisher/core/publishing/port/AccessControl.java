package com.sitepark.ies.publisher.core.publishing.port;

/** Authorization contract for the publishing operations of the current user. */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AccessControl {

  /**
   * Returns whether the current user may look at and change how the server publishes: the number
   * of workers, for example.
   *
   * @return {@code true} if managing publishing is permitted
   */
  boolean isAllowManagePublishing();
}
