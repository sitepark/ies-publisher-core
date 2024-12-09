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

class LinkCheckerLinkTest {

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerLink.class).verify();
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  public void testToString() {
    ToStringVerifier.forClass(LinkCheckerLink.class).withClassName(NameStyle.SIMPLE_NAME).verify();
  }

  @Test
  public void testToBuilder() {
    LinkCheckerLink link =
        LinkCheckerLink.builder().hash("hash").url("http://example.com").timeout(5).build();

    LinkCheckerLink copy = link.toBuilder().timeout(10).build();

    LinkCheckerLink expected =
        LinkCheckerLink.builder().hash("hash").url("http://example.com").timeout(10).build();

    assertEquals(expected, copy, "Unexpected link after toBuilder");
  }

  @Test
  void testSerialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    LinkCheckerLink link =
        LinkCheckerLink.builder().hash("hash").url("http://example.com").timeout(5).build();

    String json = mapper.writeValueAsString(link);
    assertEquals(
        "{\"hash\":\"hash\",\"url\":\"http://example.com\",\"timeout\":5}",
        json,
        "Unexpected JSON output");
  }

  @Test
  void testDeserialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    String data = "{\"hash\":\"hash\",\"url\":\"http://example.com\",\"timeout\":5}";

    LinkCheckerLink link = mapper.readValue(data, LinkCheckerLink.class);

    LinkCheckerLink expected =
        LinkCheckerLink.builder().hash("hash").url("http://example.com").timeout(5).build();

    assertEquals(expected, link, "Unexpected link deserialization");
  }
}
