package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class LinkCheckerResultItemTest {

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerResultItem.class).verify();
  }
}
