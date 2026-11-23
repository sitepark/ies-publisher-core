package com.sitepark.ies.publisher.core.translation.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TranslatedStatisticEntryTest {

  @Test
  void testBuilder() {
    TranslatedStatisticEntry entry =
        TranslatedStatisticEntry.builder()
            .period(LocalDate.of(2026, 11, 1))
            .language("en")
            .timeMillis(250)
            .bytes(1024)
            .build();
    assertThat(entry)
        .isEqualTo(new TranslatedStatisticEntry(LocalDate.of(2026, 11, 1), "en", 250, 1024));
  }

  @Test
  void testNegativeTime() {
    TranslatedStatisticEntry.Builder builder =
        TranslatedStatisticEntry.builder()
            .period(LocalDate.of(2026, 11, 1))
            .language("en")
            .timeMillis(-1);
    assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testNegativeBytes() {
    TranslatedStatisticEntry.Builder builder =
        TranslatedStatisticEntry.builder()
            .period(LocalDate.of(2026, 11, 1))
            .language("en")
            .bytes(-1);
    assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testLanguageRequired() {
    TranslatedStatisticEntry.Builder builder =
        TranslatedStatisticEntry.builder().period(LocalDate.of(2026, 11, 1));
    assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
  }
}
