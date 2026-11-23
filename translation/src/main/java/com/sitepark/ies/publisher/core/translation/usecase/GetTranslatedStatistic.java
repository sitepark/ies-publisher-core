package com.sitepark.ies.publisher.core.translation.usecase;

import com.sitepark.ies.publisher.core.translation.domain.entity.TranslatedStatisticEntry;
import com.sitepark.ies.publisher.core.translation.domain.entity.TranslationStatisticFilter;
import com.sitepark.ies.publisher.core.translation.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.translation.port.AccessControl;
import com.sitepark.ies.publisher.core.translation.port.TranslationStatisticRepository;
import jakarta.inject.Inject;
import java.util.List;

/** Use case that returns the volume of texts that were translated, per period and target language. */
public class GetTranslatedStatistic {

  private final AccessControl accessControl;
  private final TranslationStatisticRepository repository;

  /**
   * Creates the use case with its required dependencies.
   *
   * @param accessControl checks whether the current user may read the statistics
   * @param repository provides the recorded statistics
   */
  @Inject
  public GetTranslatedStatistic(
      AccessControl accessControl, TranslationStatisticRepository repository) {
    this.accessControl = accessControl;
    this.repository = repository;
  }

  /**
   * Returns the volume of texts that were translated, per period and target language.
   *
   * @param filter the requested periods
   * @return the statistic entries, or an empty list
   * @throws AccessDeniedException if the current user may not read the statistics
   */
  public List<TranslatedStatisticEntry> getTranslatedStatistic(TranslationStatisticFilter filter) {
    if (!this.accessControl.isAllowGetTranslationStatistic()) {
      throw new AccessDeniedException("Not allowed to get translation statistic");
    }
    return this.repository.getTranslated(filter);
  }
}
