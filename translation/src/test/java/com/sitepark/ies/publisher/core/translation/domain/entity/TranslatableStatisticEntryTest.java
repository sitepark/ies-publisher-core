package com.sitepark.ies.publisher.core.translation.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TranslatableStatisticEntryTest {

  @Test
  void testBuilder() {
    TranslatableStatisticEntry entry =
        TranslatableStatisticEntry.builder()
            .period(LocalDate.of(2026, 11, 1))
            .publisherId("3")
            .bytes(1024)
            .build();
    assertThat(entry)
        .isEqualTo(new TranslatableStatisticEntry(LocalDate.of(2026, 11, 1), "3", null, 1024));
  }

  @Test
  void testNegativeBytes() {
    TranslatableStatisticEntry.Builder builder =
        TranslatableStatisticEntry.builder()
            .period(LocalDate.of(2026, 11, 1))
            .publisherId("3")
            .bytes(-1);
    assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testPublisherIdRequired() {
    TranslatableStatisticEntry.Builder builder =
        TranslatableStatisticEntry.builder().period(LocalDate.of(2026, 11, 1));
    assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
  }
}
