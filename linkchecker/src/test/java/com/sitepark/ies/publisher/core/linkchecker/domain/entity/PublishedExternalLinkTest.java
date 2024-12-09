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

class PublishedExternalLinkTest {

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testEquals() {
    EqualsVerifier.forClass(PublishedExternalLink.class).verify();
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  public void testToString() {
    ToStringVerifier.forClass(PublishedExternalLink.class)
        .withClassName(NameStyle.SIMPLE_NAME)
        .verify();
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

  @Test
  void testToBuilder() {
    PublishedExternalLink link =
        PublishedExternalLink.builder()
            .entity("123")
            .channel("2")
            .section("abc")
            .url("http://example.com")
            .build()
            .toBuilder()
            .section("def")
            .build();

    PublishedExternalLink expected =
        PublishedExternalLink.builder()
            .entity("123")
            .channel("2")
            .section("def")
            .url("http://example.com")
            .build();

    assertEquals(expected, link, "Unexpected link after toBuilder");
  }

  @Test
  void testDeserialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    String data =
        "{\"entity\":\"123\",\"channel\":\"2\",\"section\":\"abc\",\"url\":\"http://example.com\"}";

    PublishedExternalLink link = mapper.readValue(data, PublishedExternalLink.class);

    PublishedExternalLink expected =
        PublishedExternalLink.builder()
            .url("http://example.com")
            .channel("2")
            .section("abc")
            .entity("123")
            .build();

    assertEquals(expected, link, "Unexpected link deserialization");
  }

  @Test
  void testSerialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    PublishedExternalLink link =
        PublishedExternalLink.builder()
            .url("http://example.com")
            .channel("2")
            .section("abc")
            .entity("123")
            .build();

    String json = mapper.writeValueAsString(link);
    assertEquals(
        "{\"entity\":\"123\",\"channel\":\"2\",\"section\":\"abc\",\"url\":\"http://example.com\"}",
        json,
        "Unexpected JSON output");
  }
}
