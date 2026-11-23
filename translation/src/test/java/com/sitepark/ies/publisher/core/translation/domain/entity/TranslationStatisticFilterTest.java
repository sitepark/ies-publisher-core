package com.sitepark.ies.publisher.core.translation.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TranslationStatisticFilterTest {

  @Test
  void testBuilderDefaultsToMonth() {
    TranslationStatisticFilter filter =
        TranslationStatisticFilter.builder()
            .from(LocalDate.of(2026, 1, 1))
            .to(LocalDate.of(2026, 11, 30))
            .build();
    assertThat(filter.granularity()).isEqualTo(StatisticGranularity.MONTH);
  }

  @Test
  void testSameDayIsValid() {
    LocalDate day = LocalDate.of(2026, 11, 1);
    TranslationStatisticFilter filter =
        TranslationStatisticFilter.builder().from(day).to(day).build();
    assertThat(filter.from()).isEqualTo(filter.to());
  }

  @Test
  void testToBeforeFrom() {
    TranslationStatisticFilter.Builder builder =
        TranslationStatisticFilter.builder()
            .from(LocalDate.of(2026, 2, 1))
            .to(LocalDate.of(2026, 1, 31));
    assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testFromRequired() {
    TranslationStatisticFilter.Builder builder =
        TranslationStatisticFilter.builder().to(LocalDate.of(2026, 1, 31));
    assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
  }
}
