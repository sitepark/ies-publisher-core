package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

class LinkCheckerResultTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerResult.class).verify();
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(LinkCheckerResult.class)
        .withClassName(NameStyle.SIMPLE_NAME)
        .verify();
  }

  @Test
  void testToBuilder() {
    LinkCheckerResult result =
        LinkCheckerResult.builder()
            .total(2)
            .start(1)
            .limit(3)
            .item(LinkCheckerResultItem.builder().url("https://example.com").build())
            .build()
            .toBuilder()
            .limit(4)
            .build();

    LinkCheckerResult expected =
        LinkCheckerResult.builder()
            .total(2)
            .start(1)
            .limit(4)
            .item(LinkCheckerResultItem.builder().url("https://example.com").build())
            .build();

    assertEquals(expected, result, "Unexpected copy after toBuilder");
  }

  @Test
  void testSerialize() {

    JsonMapper mapper =
        JsonMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

    LinkCheckerResult result =
        LinkCheckerResult.builder()
            .total(2)
            .start(1)
            .limit(3)
            .item(LinkCheckerResultItem.builder().url("https://example.com").build())
            .build();

    String json = mapper.writeValueAsString(result);
    assertEquals(
        "{\"items\":[{\"url\":\"https://example.com\",\"hash\":null,\"status\":null,\"message\":null,\"entities\":[]}],\"total\":2,\"start\":1,\"limit\":3}",
        json,
        "Unexpected JSON output");
  }
}
