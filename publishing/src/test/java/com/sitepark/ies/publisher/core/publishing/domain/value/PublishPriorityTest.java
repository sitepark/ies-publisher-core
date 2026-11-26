package com.sitepark.ies.publisher.core.publishing.domain.value;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class PublishPriorityTest {

  @Test
  void urgentIsAtLeastAsUrgentAsBulk() {
    assertThat(PublishPriority.URGENT.isAtLeast(PublishPriority.BULK))
        .as("urgent work should be handed out no later than bulk work")
        .isTrue();
  }

  @Test
  void normalLiesBetweenUrgentAndBulk() {
    assertThat(PublishPriority.NORMAL.isAtLeast(PublishPriority.BULK))
        .as("what is due should go before bulk work")
        .isTrue();
    assertThat(PublishPriority.NORMAL.isAtLeast(PublishPriority.URGENT))
        .as("what is due should not count as urgent")
        .isFalse();
  }

  @Test
  void higher_returnsTheMoreUrgentPriority() {
    assertThat(PublishPriority.BULK.higher(PublishPriority.NORMAL))
        .as("the more urgent of both priorities should win")
        .isEqualTo(PublishPriority.NORMAL);
  }
}
