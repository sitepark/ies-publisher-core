package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class LinkCheckerBackgroundExecutionTest {

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  void testEquals() {
    EqualsVerifier.forClass(LinkCheckerBackgroundExecution.class).verify();
  }

  @Test
  @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
  public void testToString() {
    ToStringVerifier.forClass(LinkCheckerBackgroundExecution.class)
        .withClassName(NameStyle.SIMPLE_NAME)
        .verify();
  }

  @Test
  void testMissintTopic() {
    assertThrows(
        IllegalStateException.class, () -> LinkCheckerBackgroundExecution.builder().build());
  }

  @Test
  void testParallel() {
    LinkCheckerBackgroundExecution execution =
        LinkCheckerBackgroundExecution.builder().parallel(2).topic("test").build();

    assertEquals(2, execution.getParallel(), "Unexpected parallel");
  }

  @Test
  void testTopic() {
    LinkCheckerBackgroundExecution execution =
        LinkCheckerBackgroundExecution.builder().topic(new String[] {"topic1", "topic2"}).build();

    assertArrayEquals(new String[] {"topic1", "topic2"}, execution.getTopic(), "Unexpected topic");
  }

  @Test
  void testLinks() {
    LinkCheckerBackgroundExecution execution =
        LinkCheckerBackgroundExecution.builder()
            .topic("test")
            .links(
                List.of(
                    LinkCheckerLink.builder().url("http://example.com").build(),
                    LinkCheckerLink.builder().url("http://example.org").build()))
            .build();

    assertEquals(
        List.of(
            LinkCheckerLink.builder().url("http://example.com").build(),
            LinkCheckerLink.builder().url("http://example.org").build()),
        execution.getLinks(),
        "Unexpected links");
  }

  @Test
  void testOperation() {
    LinkCheckerBackgroundExecution execution =
        LinkCheckerBackgroundExecution.builder().topic("test").operation(link -> {}).build();

    assertNotNull(execution.getOperation(), "operation not set");
  }

  @Test
  void testToBuilder() {
    LinkCheckerBackgroundExecution execution =
        LinkCheckerBackgroundExecution.builder()
            .parallel(2)
            .topic(new String[] {"topic1", "topic2"})
            .link(LinkCheckerLink.builder().url("http://example.com").build())
            .build()
            .toBuilder()
            .parallel(3)
            .build();

    LinkCheckerBackgroundExecution expected =
        LinkCheckerBackgroundExecution.builder()
            .parallel(3)
            .topic(new String[] {"topic1", "topic2"})
            .link(LinkCheckerLink.builder().url("http://example.com").build())
            .build();

    assertEquals(expected, execution, "Unexpected copy after toBuilder");
  }
}
