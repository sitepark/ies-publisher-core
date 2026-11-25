package com.sitepark.ies.publisher.core.publishing.domain.value;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PublishPriorityTest {

  @Test
  void dependencyIsAtLeastAsUrgentAsBulk() {
    assertThat(PublishPriority.DEPENDENCY.isAtLeast(PublishPriority.BULK))
        .as("a dependency should be handed out no later than bulk work")
        .isTrue();
  }

  @Test
  void bulkIsLessUrgentThanDependency() {
    assertThat(PublishPriority.BULK.isAtLeast(PublishPriority.DEPENDENCY))
        .as("bulk work should not count as urgent as a dependency")
        .isFalse();
  }

  @Test
  void higher_returnsTheMoreUrgentPriority() {
    assertThat(PublishPriority.BULK.higher(PublishPriority.DEPENDENCY))
        .as("the more urgent of both priorities should win")
        .isEqualTo(PublishPriority.DEPENDENCY);
  }
}
