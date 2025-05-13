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

class LinkCheckerResultItemTest {

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerResultItem.class).verify();
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
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
  void testDeserialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    String data =
        "{\"url\":\"https://example.com\",\"hash\":\"hash\",\"status\":\"OK\",\"message\":\"message\",\"entities\":[\"123\",\"345\"]}";

    LinkCheckerResultItem item = mapper.readValue(data, LinkCheckerResultItem.class);

    LinkCheckerResultItem expected =
        LinkCheckerResultItem.builder()
            .url("https://example.com")
            .hash("hash")
            .status(StatusType.OK)
            .message("message")
            .entity("123")
            .entity("345")
            .build();

    assertEquals(expected, item, "Unexpected item deserialization");
  }

  @Test
  void testSerialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

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
