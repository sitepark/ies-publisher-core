package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class LinkCheckerResultTest {

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerResult.class).verify();
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  public void testToString() {
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
            .item(LinkCheckerResultItem.builder().url("http://example.com").build())
            .build()
            .toBuilder()
            .limit(4)
            .build();

    LinkCheckerResult expected =
        LinkCheckerResult.builder()
            .total(2)
            .start(1)
            .limit(4)
            .item(LinkCheckerResultItem.builder().url("http://example.com").build())
            .build();

    assertEquals(expected, result, "Unexpected copy after toBuilder");
  }

  @Test
  void testSerialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    LinkCheckerResult result =
        LinkCheckerResult.builder()
            .total(2)
            .start(1)
            .limit(3)
            .item(LinkCheckerResultItem.builder().url("http://example.com").build())
            .build();

    String json = mapper.writeValueAsString(result);
    assertEquals(
        "{\"items\":[{\"url\":\"http://example.com\",\"hash\":null,\"status\":null,\"message\":null,\"entities\":[]}],\"total\":2,\"start\":1,\"limit\":3}",
        json,
        "Unexpected JSON output");
  }

  @Test
  void testDeserialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    String data =
        "{\"items\":[{\"url\":\"http://example.com\",\"hash\":null,\"status\":null,\"message\":null,\"entities\":[]}],\"total\":2,\"start\":1,\"limit\":3}";

    LinkCheckerResult result = mapper.readValue(data, LinkCheckerResult.class);

    LinkCheckerResult expected =
        LinkCheckerResult.builder()
            .total(2)
            .start(1)
            .limit(3)
            .item(LinkCheckerResultItem.builder().url("http://example.com").build())
            .build();

    assertEquals(expected, result, "Unexpected result deserialization");
  }
}
