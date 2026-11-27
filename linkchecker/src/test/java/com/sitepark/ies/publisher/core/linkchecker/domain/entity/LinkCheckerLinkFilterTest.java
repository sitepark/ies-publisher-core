package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import java.util.Arrays;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

class LinkCheckerLinkFilterTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerLinkFilter.class).verify();
  }

  @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
  @Test
  void testBuilderReuseDoesNotChangeBuiltFilter() {
    LinkCheckerLinkFilter.Builder builder = LinkCheckerLinkFilter.builder().term("a");
    LinkCheckerLinkFilter filter = builder.build();
    builder.term("b").statusType(StatusType.OK);
    assertEquals(
        List.of("a"), filter.getTerms(), "A built filter must not change with its builder");
    assertEquals(
        List.of(), filter.getStatusTypes(), "A built filter must not change with its builder");
  }

  @Test
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
  void testSerialize() {

    JsonMapper mapper =
        JsonMapper.builder().disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();

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
}
