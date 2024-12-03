package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class LinkCheckerConfigTest {

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerConfig.class).verify();
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
            .parallel(3)
            .timeout(10)
            .scheduling(DailyScheduling.builder().startTime(3, 0).build())
            .exclude(new LinkCheckerExcludePattern(LinkCheckerExcludePatternType.REGEX, ".*\\.pdf"))
            .build();

    String json = mapper.writeValueAsString(config);

    assertEquals(
        "{\"parallel\":3,\"timeout\":10,\"scheduling\":{\"startTime\":\"03:00\",\"type\":\"daily\"},\"excludes\":[{\"type\":\"REGEX\",\"pattern\":\".*\\\\.pdf\"}]}",
        json,
        "Unexpected JSON output");
  }
}
