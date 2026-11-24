package com.sitepark.ies.publisher.core.translation.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.translation.domain.entity.TranslatableStatisticEntry;
import com.sitepark.ies.publisher.core.translation.domain.entity.TranslationStatisticFilter;
import com.sitepark.ies.publisher.core.translation.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.translation.port.AccessControl;
import com.sitepark.ies.publisher.core.translation.port.PublisherNameResolver;
import com.sitepark.ies.publisher.core.translation.port.TranslationStatisticRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetTranslatableStatisticTest {

  private static final TranslationStatisticFilter FILTER =
      TranslationStatisticFilter.builder()
          .from(LocalDate.of(2026, 1, 1))
          .to(LocalDate.of(2026, 11, 30))
          .build();

  private static TranslatableStatisticEntry entry(int month, String publisherId) {
    return TranslatableStatisticEntry.builder()
        .period(LocalDate.of(2026, month, 1))
        .publisherId(publisherId)
        .bytes(100)
        .build();
  }

  @Test
  void testAccessDenied() {
    AccessControl accessControl = mock();
    GetTranslatableStatistic useCase = new GetTranslatableStatistic(accessControl, mock(), mock());

    assertThatThrownBy(() -> useCase.getTranslatableStatistic(FILTER))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void testAddsPublisherNames() {
    AccessControl accessControl = mock();
    when(accessControl.isAllowGetTranslationStatistic()).thenReturn(true);
    TranslationStatisticRepository repository = mock();
    when(repository.getTranslatable(FILTER)).thenReturn(List.of(entry(1, "3"), entry(2, "4")));
    PublisherNameResolver resolver = mock();
    when(resolver.getName("3")).thenReturn(Optional.of("Internet"));
    when(resolver.getName("4")).thenReturn(Optional.empty());
    GetTranslatableStatistic useCase =
        new GetTranslatableStatistic(accessControl, repository, resolver);

    assertThat(useCase.getTranslatableStatistic(FILTER))
        .containsExactly(
            entry(1, "3").withPublisherName("Internet"), entry(2, "4").withPublisherName(null));
  }

  @Test
  void testResolvesEachPublisherOnce() {
    AccessControl accessControl = mock();
    when(accessControl.isAllowGetTranslationStatistic()).thenReturn(true);
    TranslationStatisticRepository repository = mock();
    when(repository.getTranslatable(FILTER)).thenReturn(List.of(entry(1, "3"), entry(2, "3")));
    PublisherNameResolver resolver = mock();
    when(resolver.getName("3")).thenReturn(Optional.of("Internet"));
    GetTranslatableStatistic useCase =
        new GetTranslatableStatistic(accessControl, repository, resolver);

    useCase.getTranslatableStatistic(FILTER);

    verify(resolver, times(1)).getName("3");
  }
}
