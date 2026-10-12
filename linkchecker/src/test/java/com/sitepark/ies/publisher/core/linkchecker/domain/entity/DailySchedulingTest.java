package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.InvalidLinkCheckerConfigException;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class DailySchedulingTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(DailyScheduling.class).verify();
  }

  @Test
  void testToBuilder() {
    DailyScheduling scheduling = DailyScheduling.builder().startTime(3, 0).build();

    DailyScheduling copy = scheduling.toBuilder().build();

    assertEquals(scheduling, copy, "Unexpected copy after toBuilder");
  }

  @Test
  void testToString() {
    DailyScheduling scheduling = DailyScheduling.builder().startTime(3, 0).build();

    assertEquals(
        "DailyScheduling [startTime=03:00]", scheduling.toString(), "Unexpected toString output");
  }

  @Test
  void testSerialize() throws JsonProcessingException {
    DailyScheduling scheduling = DailyScheduling.builder().startTime(3, 0).build();

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    String json = mapper.writeValueAsString(scheduling);
    assertEquals(
        "{\"startTime\":\"03:00\",\"type\":\"daily\"}", json, "Unexpected JSON serialization");
  }

  @Test
  void testDeserialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    String data = "{\"startTime\":\"03:00\",\"type\":\"daily\"}";

    DailyScheduling scheduling = mapper.readValue(data, DailyScheduling.class);

    DailyScheduling expected = DailyScheduling.builder().startTime(3, 0).build();

    assertEquals(expected, scheduling, "Unexpected scheduling deserialization");
  }

  @Test
  void testStartTimeFromString() {
    DailyScheduling scheduling = DailyScheduling.builder().startTime("08:30").build();
    assertEquals(
        DailyScheduling.builder().startTime(8, 30).build(), scheduling, "Unexpected start time");
  }

  @Test
  void testStartTimeFromStringOutOfRange() {
    assertThrows(
        InvalidLinkCheckerConfigException.class,
        () -> DailyScheduling.builder().startTime("25:00"),
        "25:00 must be rejected");
  }

  @Test
  void testStartTimeFromStringNotATime() {
    assertThrows(
        InvalidLinkCheckerConfigException.class,
        () -> DailyScheduling.builder().startTime("abc"),
        "abc must be rejected");
  }

  @Test
  void testStartTimeFromStringBlank() {
    assertThrows(
        InvalidLinkCheckerConfigException.class,
        () -> DailyScheduling.builder().startTime(" "),
        "Blank must be rejected");
  }

  @Test
  void testStartTimeFromStringNull() {
    assertThrows(
        InvalidLinkCheckerConfigException.class,
        () -> DailyScheduling.builder().startTime((String) null),
        "Null must be rejected");
  }

  @Test
  void testStartTimeInvalidHourMinute() {
    assertThrows(
        InvalidLinkCheckerConfigException.class,
        () -> DailyScheduling.builder().startTime(24, 0),
        "Hour 24 must be rejected");
  }
}
