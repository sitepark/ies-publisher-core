package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

class LinkCheckerResultStatisticTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerResultStatistic.class).verify();
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(LinkCheckerResultStatistic.class)
        .withClassName(NameStyle.SIMPLE_NAME)
        .verify();
  }

  @Test
  void testToBuilder() {
    LinkCheckerResultStatistic statistic =
        LinkCheckerResultStatistic.builder()
            .statusCount(new StatusTypeCount(StatusType.OK, 5))
            .build()
            .toBuilder()
            .build();

    LinkCheckerResultStatistic expected =
        LinkCheckerResultStatistic.builder()
            .statusCount(new StatusTypeCount(StatusType.OK, 5))
            .build();

    assertEquals(expected, statistic, "Unexpected statistic after toBuilder");
  }

  @Test
  void testSerialize() {

    JsonMapper mapper =
        JsonMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

    LinkCheckerResultStatistic statistic =
        LinkCheckerResultStatistic.builder()
            .statusCount(new StatusTypeCount(StatusType.OK, 5))
            .build()
            .toBuilder()
            .build();

    String json = mapper.writeValueAsString(statistic);
    assertEquals(
        "{\"statusCounts\":[{\"statusType\":\"OK\",\"count\":5}]}", json, "Unexpected JSON output");
  }
}
