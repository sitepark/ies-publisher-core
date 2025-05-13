package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import java.util.Arrays;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class LinkCheckerLinkFilterTest {

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerLinkFilter.class).verify();
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testToString() {
    ToStringVerifier.forClass(LinkCheckerLinkFilter.class)
        .withClassName(NameStyle.SIMPLE_NAME)
        .verify();
  }

  @Test
  void testTermsArray() {
    LinkCheckerLinkFilter filter = LinkCheckerLinkFilter.builder().terms("term1", "term2").build();

    assertEquals(Arrays.asList("term1", "term2"), filter.getTerms(), "Unexpected terms");
  }

  @Test
  void testToBuilder() {
    LinkCheckerLinkFilter filter =
        LinkCheckerLinkFilter.builder()
            .terms(List.of("term1", "term2"))
            .statusTypes(List.of(StatusType.UNKNOWN))
            .build()
            .toBuilder()
            .statusTypes(List.of(StatusType.OK))
            .build();

    LinkCheckerLinkFilter expected =
        LinkCheckerLinkFilter.builder()
            .terms(List.of("term1", "term2"))
            .statusTypes(List.of(StatusType.OK))
            .build();

    assertEquals(expected, filter, "Unexpected copy after toBuilder");
  }

  @Test
  void testSerialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    LinkCheckerLinkFilter filter =
        LinkCheckerLinkFilter.builder()
            .terms(List.of("term1", "term2"))
            .statusTypes(List.of(StatusType.OK))
            .build();

    String json = mapper.writeValueAsString(filter);
    assertEquals(
        "{\"terms\":[\"term1\",\"term2\"],\"statusTypes\":[\"OK\"]}",
        json,
        "Unexpected JSON output");
  }

  @Test
  void testDeserialize() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());

    String data = "{\"terms\":[\"term1\",\"term2\"],\"statusTypes\":[\"OK\"]}";

    LinkCheckerLinkFilter filter = mapper.readValue(data, LinkCheckerLinkFilter.class);

    LinkCheckerLinkFilter expected =
        LinkCheckerLinkFilter.builder()
            .terms(List.of("term1", "term2"))
            .statusTypes(List.of(StatusType.OK))
            .build();

    assertEquals(expected, filter, "Unexpected filter deserialization");
  }
}
