package com.sitepark.ies.publisher.core.linkchecker.port;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLink;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultItem;

/** Contract for validating a single external link by performing the actual network request. */
public interface LinkChecker {
  /**
   * Checks the given link and returns the outcome of the validation.
   *
   * @param link the link to validate
   * @return the result of checking the link
   */
  LinkCheckerResultItem checkLink(LinkCheckerLink link);
}
