package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class PublishedExternalLinkTest {

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testEquals() {
    EqualsVerifier.forClass(PublishedExternalLink.class).verify();
  }

  @Test
  void testSetEntity() {
    PublishedExternalLink link = PublishedExternalLink.builder().entity("entity").build();

    assertEquals("entity", link.getEntity(), "unexpected entity");
  }

  @Test
  void testSetSection() {
    PublishedExternalLink link = PublishedExternalLink.builder().section("section").build();

    assertEquals("section", link.getSection(), "unexpected section");
  }

  @Test
  void testSetUrl() {
    PublishedExternalLink link = PublishedExternalLink.builder().url("http://example.com").build();

    assertEquals("http://example.com", link.getUrl(), "unexpected url");
  }
}
