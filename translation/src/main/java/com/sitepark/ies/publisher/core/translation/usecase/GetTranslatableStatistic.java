package com.sitepark.ies.publisher.core.translation.usecase;

import com.sitepark.ies.publisher.core.translation.domain.entity.TranslatableStatisticEntry;
import com.sitepark.ies.publisher.core.translation.domain.entity.TranslationStatisticFilter;
import com.sitepark.ies.publisher.core.translation.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.translation.port.AccessControl;
import com.sitepark.ies.publisher.core.translation.port.PublisherNameResolver;
import com.sitepark.ies.publisher.core.translation.port.TranslationStatisticRepository;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Use case that returns the volume of texts that would have been translated, per period and
 * publisher, together with the display name of each publisher.
 */
public class GetTranslatableStatistic {

  private final AccessControl accessControl;
  private final TranslationStatisticRepository repository;
  private final PublisherNameResolver publisherNameResolver;

  /**
   * Creates the use case with its required dependencies.
   *
   * @param accessControl checks whether the current user may read the statistics
   * @param repository provides the recorded statistics
   * @param publisherNameResolver resolves the display names of the publishers
   */
  @Inject
  public GetTranslatableStatistic(
      AccessControl accessControl,
      TranslationStatisticRepository repository,
      PublisherNameResolver publisherNameResolver) {
    this.accessControl = accessControl;
    this.repository = repository;
    this.publisherNameResolver = publisherNameResolver;
  }

  /**
   * Returns the volume of texts that would have been translated, per period and publisher.
   *
   * @param filter the requested periods
   * @return the statistic entries with publisher names, or an empty list
   * @throws AccessDeniedException if the current user may not read the statistics
   */
  public List<TranslatableStatisticEntry> getTranslatableStatistic(
      TranslationStatisticFilter filter) {
    if (!this.accessControl.isAllowGetTranslationStatistic()) {
      throw new AccessDeniedException("Not allowed to get translation statistic");
    }
    @SuppressWarnings("PMD.UseConcurrentHashMap") // local, used by a sequential stream only
    Map<String, Optional<String>> names = new HashMap<>();
    return this.repository.getTranslatable(filter).stream()
        .map(
            entry ->
                entry.withPublisherName(
                    names
                        .computeIfAbsent(entry.publisherId(), this.publisherNameResolver::getName)
                        .orElse(null)))
        .toList();
  }
}
