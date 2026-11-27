package com.sitepark.ies.publisher.core.translation.domain.entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Selects the periods of a translation statistic query.
 *
 * <p>{@code from} and {@code to} are inclusive. With {@link StatisticGranularity#MONTH} only the
 * year and month of both dates are evaluated.
 *
 * @param from the first day of the requested time range
 * @param to the last day of the requested time range; must not be before {@code from}
 * @param granularity the granularity of the returned entries
 */
public record TranslationStatisticFilter(
    LocalDate from, LocalDate to, StatisticGranularity granularity) {

  /**
   * Validates the time range.
   *
   * @throws NullPointerException if a field is {@code null}
   * @throws IllegalArgumentException if {@code to} is before {@code from}
   */
  public TranslationStatisticFilter {
    Objects.requireNonNull(from, "from is required");
    Objects.requireNonNull(to, "to is required");
    Objects.requireNonNull(granularity, "granularity is required");
    if (to.isBefore(from)) {
      throw new IllegalArgumentException("to (" + to + ") must not be before from (" + from + ")");
    }
  }

  /**
   * Creates a new builder for {@link TranslationStatisticFilter}.
   *
   * @return a new builder with granularity {@link StatisticGranularity#MONTH}
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for {@link TranslationStatisticFilter}. */
  @SuppressWarnings("NullAway.Init") // required fields are set after construction
  public static final class Builder {
    private LocalDate from;
    private LocalDate to;
    private StatisticGranularity granularity = StatisticGranularity.MONTH;

    private Builder() {}

    /**
     * Sets the first day of the time range.
     *
     * @param from the first day, inclusive
     * @return this builder
     */
    public Builder from(LocalDate from) {
      this.from = from;
      return this;
    }

    /**
     * Sets the last day of the time range.
     *
     * @param to the last day, inclusive
     * @return this builder
     */
    public Builder to(LocalDate to) {
      this.to = to;
      return this;
    }

    /**
     * Sets the granularity of the returned entries.
     *
     * @param granularity the granularity
     * @return this builder
     */
    public Builder granularity(StatisticGranularity granularity) {
      this.granularity = granularity;
      return this;
    }

    /**
     * Builds the filter.
     *
     * @return the filter
     */
    public TranslationStatisticFilter build() {
      return new TranslationStatisticFilter(from, to, granularity);
    }
  }
}
