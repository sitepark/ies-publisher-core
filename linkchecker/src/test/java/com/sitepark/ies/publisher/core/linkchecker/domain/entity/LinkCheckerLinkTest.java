package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

class LinkCheckerLinkTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerLink.class).verify();
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(LinkCheckerLink.class).withClassName(NameStyle.SIMPLE_NAME).verify();
  }

  @Test
  void testToBuilder() {
    LinkCheckerLink link =
        LinkCheckerLink.builder().hash("hash").url("https://example.com").timeout(5).build();

    LinkCheckerLink copy = link.toBuilder().timeout(10).build();

    LinkCheckerLink expected =
        LinkCheckerLink.builder().hash("hash").url("https://example.com").timeout(10).build();

    assertEquals(expected, copy, "Unexpected link after toBuilder");
  }

  @Test
  void testSerialize() {

    JsonMapper mapper =
        JsonMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

    LinkCheckerLink link =
        LinkCheckerLink.builder().hash("hash").url("https://example.com").timeout(5).build();

    String json = mapper.writeValueAsString(link);
    assertEquals(
        "{\"hash\":\"hash\",\"url\":\"https://example.com\",\"timeout\":5}",
        json,
        "Unexpected JSON output");
  }
}
