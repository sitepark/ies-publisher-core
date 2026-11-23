package com.sitepark.ies.publisher.core.translation.port;

/** Authorization contract for the translation operations of the current user. */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AccessControl {

  /**
   * Returns whether the current user may read the translation statistics.
   *
   * @return {@code true} if reading the statistics is permitted
   */
  boolean isAllowGetTranslationStatistic();
}
