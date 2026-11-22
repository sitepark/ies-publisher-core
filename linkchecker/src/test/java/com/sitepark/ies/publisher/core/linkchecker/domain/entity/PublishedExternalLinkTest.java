package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

class PublishedExternalLinkTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(PublishedExternalLink.class).verify();
  }

  @Test
  void testToString() {
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
    PublishedExternalLink link = PublishedExternalLink.builder().url("https://example.com").build();

    assertEquals("https://example.com", link.getUrl(), "unexpected url");
  }

  @Test
  void testToBuilder() {
    PublishedExternalLink link =
        PublishedExternalLink.builder()
            .entity("123")
            .channel("2")
            .section("abc")
            .url("https://example.com")
            .build()
            .toBuilder()
            .section("def")
            .build();

    PublishedExternalLink expected =
        PublishedExternalLink.builder()
            .entity("123")
            .channel("2")
            .section("def")
            .url("https://example.com")
            .build();

    assertEquals(expected, link, "Unexpected link after toBuilder");
  }

  @Test
  void testSerialize() {

    JsonMapper mapper =
        JsonMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

    PublishedExternalLink link =
        PublishedExternalLink.builder()
            .url("https://example.com")
            .channel("2")
            .section("abc")
            .entity("123")
            .build();

    String json = mapper.writeValueAsString(link);
    assertEquals(
        "{\"entity\":\"123\",\"channel\":\"2\",\"section\":\"abc\",\"url\":\"https://example.com\"}",
        json,
        "Unexpected JSON output");
  }
}
