package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.InvalidLinkCheckerConfigException;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

class LinkCheckerConfigTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerConfig.class).verify();
  }

  @Test
  void testToString() {
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
  void testBuilderReuseDoesNotChangeBuiltConfig() {
    LinkCheckerConfig.Builder builder = LinkCheckerConfig.builder();
    LinkCheckerConfig config = builder.build();
    builder.exclude(new LinkCheckerExcludePattern(LinkCheckerExcludePatternType.CONTAINS, "x"));
    assertEquals(0, config.getExcludes().size(), "A built config must not change with its builder");
  }

  @Test
  void testParallelDefaultsToMinimum() {
    assertEquals(
        LinkCheckerConfig.MIN_PARALLEL,
        LinkCheckerConfig.builder().build().getParallel(),
        "Unset parallel must not be 0");
  }

  @Test
  void testNegativeTimeout() {
    assertThrows(
        InvalidLinkCheckerConfigException.class,
        () -> LinkCheckerConfig.builder().timeout(-1),
        "A negative timeout must be rejected");
  }

  @Test
  void testParallelToLow() {
    assertThrows(
        InvalidLinkCheckerConfigException.class,
        () -> LinkCheckerConfig.builder().parallel(0).build());
  }

  @Test
  void testParallelToHigh() {
    assertThrows(
        InvalidLinkCheckerConfigException.class,
        () -> LinkCheckerConfig.builder().parallel(6).build());
  }

  @Test
  void testSerialize() {

    JsonMapper mapper =
        JsonMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

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
