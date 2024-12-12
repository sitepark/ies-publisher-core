package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class LinkCheckerConfigTest {

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerConfig.class).verify();
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  public void testToString() {
    ToStringVerifier.forClass(LinkCheckerConfig.class)
        .withClassName(NameStyle.SIMPLE_NAME)
        .verify();
  }

  @Test
  void testToBuilder() {
    LinkCheckerConfig config =
        LinkCheckerConfig.builder()
            .parallel(3)
            .timeout(10)
            .scheduling(DailyScheduling.builder().startTime(3, 0).build())
            .exclude(new LinkCheckerExcludePattern(LinkCheckerExcludePatternType.REGEX, ".*\\.pdf"))
            .build()
            .toBuilder()
            .timeout(20)
            .build();

    LinkCheckerConfig expected =
        LinkCheckerConfig.builder()
            .parallel(3)
            .timeout(20)
            .scheduling(DailyScheduling.builder().startTime(3, 0).build())
            .exclude(new LinkCheckerExcludePattern(LinkCheckerExcludePatternType.REGEX, ".*\\.pdf"))
            .build();
    assertEquals(expected, config, "Unexpected config after toBuilder");
  }

  @Test
  void testParallelToLow() {
    assertThrows(
        IllegalArgumentException.class, () -> LinkCheckerConfig.builder().parallel(0).build());
  }

  @Test
  void testParallelToHigh() {
    assertThrows(
        IllegalArgumentException.class, () -> LinkCheckerConfig.builder().parallel(6).build());
  }

  @Test
  void testDeserialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    String data =
        "{\"parallel\":3,\"timeout\":10,\"scheduling\":{\"type\":\"daily\",\"startTime\":\"03:00\"},\"excludes\":[{\"type\":\"REGEX\",\"pattern\":\".*\\\\.pdf\"}]}";

    LinkCheckerConfig config = mapper.readValue(data, LinkCheckerConfig.class);

    LinkCheckerConfig expected =
        LinkCheckerConfig.builder()
            .parallel(3)
            .timeout(10)
            .scheduling(DailyScheduling.builder().startTime(3, 0).build())
            .exclude(new LinkCheckerExcludePattern(LinkCheckerExcludePatternType.REGEX, ".*\\.pdf"))
            .build();

    assertEquals(expected, config, "Unexpected config deserialization");
  }

  @Test
  void testSerialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    LinkCheckerConfig config =
        LinkCheckerConfig.builder()
            .enabled(true)
            .parallel(3)
            .timeout(10)
            .scheduling(DailyScheduling.builder().startTime(3, 0).build())
            .exclude(new LinkCheckerExcludePattern(LinkCheckerExcludePatternType.REGEX, ".*\\.pdf"))
            .build();

    String json = mapper.writeValueAsString(config);

    assertEquals(
        "{\"enabled\":true,\"parallel\":3,\"timeout\":10,\"scheduling\":{\"startTime\":\"03:00\",\"type\":\"daily\"},\"excludes\":[{\"type\":\"REGEX\",\"pattern\":\".*\\\\.pdf\"}]}",
        json,
        "Unexpected JSON output");
  }
}
