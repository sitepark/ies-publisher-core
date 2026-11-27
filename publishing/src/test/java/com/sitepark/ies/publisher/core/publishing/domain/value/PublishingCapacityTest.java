package com.sitepark.ies.publisher.core.publishing.domain.value;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PublishingCapacityTest {

  @Test
  void carriesTheNumbers() {
    assertThat(new PublishingCapacity(4, 2))
        .as("numbers within their range should be kept as they are")
        .isEqualTo(new PublishingCapacity(4, 2))
        .extracting(PublishingCapacity::parallel, PublishingCapacity::parallelWhileInteractive)
        .containsExactly(4, 2);
  }

  @Test
  void parallelBelowOne_countsAsOne() {
    assertThat(new PublishingCapacity(0, 0).parallel())
        .as("a number of parallel tasks below 1 should count as 1")
        .isEqualTo(1);
  }

  @Test
  void negativeWhileInteractive_countsAsZero() {
    assertThat(new PublishingCapacity(1, -1).parallelWhileInteractive())
        .as("a negative number while publishing in the foreground should count as 0")
        .isZero();
  }
}
