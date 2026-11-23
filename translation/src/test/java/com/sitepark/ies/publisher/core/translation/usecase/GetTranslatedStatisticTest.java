package com.sitepark.ies.publisher.core.translation.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.translation.domain.entity.TranslatedStatisticEntry;
import com.sitepark.ies.publisher.core.translation.domain.entity.TranslationStatisticFilter;
import com.sitepark.ies.publisher.core.translation.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.translation.port.AccessControl;
import com.sitepark.ies.publisher.core.translation.port.TranslationStatisticRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetTranslatedStatisticTest {

  private static final TranslationStatisticFilter FILTER =
      TranslationStatisticFilter.builder()
          .from(LocalDate.of(2026, 1, 1))
          .to(LocalDate.of(2026, 11, 30))
          .build();

  @Test
  void testAccessDenied() {
    AccessControl accessControl = mock();
    TranslationStatisticRepository repository = mock();
    GetTranslatedStatistic useCase = new GetTranslatedStatistic(accessControl, repository);

    assertThatThrownBy(() -> useCase.getTranslatedStatistic(FILTER))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void testReturnsEntriesOfRepository() {
    AccessControl accessControl = mock();
    when(accessControl.isAllowGetTranslationStatistic()).thenReturn(true);
    TranslationStatisticRepository repository = mock();
    List<TranslatedStatisticEntry> entries = List.of(mock(TranslatedStatisticEntry.class));
    when(repository.getTranslated(FILTER)).thenReturn(entries);
    GetTranslatedStatistic useCase = new GetTranslatedStatistic(accessControl, repository);

    assertThat(useCase.getTranslatedStatistic(FILTER)).isSameAs(entries);
  }
}
