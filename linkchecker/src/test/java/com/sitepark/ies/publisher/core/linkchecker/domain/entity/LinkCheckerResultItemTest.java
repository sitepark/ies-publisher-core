package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

class LinkCheckerResultItemTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerResultItem.class).verify();
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(LinkCheckerResultItem.class)
        .withClassName(NameStyle.SIMPLE_NAME)
        .verify();
  }

  @Test
  void testToBuilder() {
    LinkCheckerResultItem item =
        LinkCheckerResultItem.builder()
            .url("https://example.com")
            .hash("hash")
            .status(StatusType.OK)
            .message("message")
            .entity("123")
            .entity("345")
            .build()
            .toBuilder()
            .message("new message")
            .build();

    LinkCheckerResultItem expected =
        LinkCheckerResultItem.builder()
            .url("https://example.com")
            .hash("hash")
            .status(StatusType.OK)
            .message("new message")
            .entity("123")
            .entity("345")
            .build();
    assertEquals(expected, item, "Unexpected item after toBuilder");
  }

  @Test
  void testSerialize() {

    JsonMapper mapper =
        JsonMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

    LinkCheckerResultItem item =
        LinkCheckerResultItem.builder()
            .url("https://example.com")
            .hash("hash")
            .status(StatusType.OK)
            .message("message")
            .entity("123")
            .entity("345")
            .build();

    String json = mapper.writeValueAsString(item);
    assertEquals(
        "{\"url\":\"https://example.com\",\"hash\":\"hash\",\"status\":\"OK\",\"message\":\"message\",\"entities\":[\"123\",\"345\"]}",
        json,
        "Unexpected JSON output");
  }
}
