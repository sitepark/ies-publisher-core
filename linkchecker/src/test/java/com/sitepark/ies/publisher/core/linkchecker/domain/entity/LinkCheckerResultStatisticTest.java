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

class LinkCheckerResultStatisticTest {

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerResultStatistic.class).verify();
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  public void testToString() {
    ToStringVerifier.forClass(LinkCheckerResultStatistic.class)
        .withClassName(NameStyle.SIMPLE_NAME)
        .verify();
  }

  @Test
  public void testToBuilder() {
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
  void testSerialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

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

  @Test
  void testDeserialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    String data = "{\"statusCounts\":[{\"statusType\":\"OK\",\"count\":5}]}";

    LinkCheckerResultStatistic statistic = mapper.readValue(data, LinkCheckerResultStatistic.class);

    LinkCheckerResultStatistic expected =
        LinkCheckerResultStatistic.builder()
            .statusCount(new StatusTypeCount(StatusType.OK, 5))
            .build()
            .toBuilder()
            .build();

    assertEquals(expected, statistic, "Unexpected statistic deserialization");
  }
}
