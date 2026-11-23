package com.sitepark.ies.publisher.core.translation.domain.entity;

import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Volume of texts that were translated into one target language in one period.
 *
 * @param period the first day of the period
 * @param language the target language as ISO 639 code, e.g. {@code en}
 * @param timeMillis the time the translation provider needed, in milliseconds
 * @param bytes the size of the translated source texts in bytes
 */
public record TranslatedStatisticEntry(
    LocalDate period, String language, long timeMillis, long bytes) {

  /**
   * Validates the entry.
   *
   * @throws NullPointerException if {@code period} or {@code language} is {@code null}
   * @throws IllegalArgumentException if {@code timeMillis} or {@code bytes} is negative
   */
  public TranslatedStatisticEntry {
    Objects.requireNonNull(period, "period is required");
    Objects.requireNonNull(language, "language is required");
    if (timeMillis < 0) {
      throw new IllegalArgumentException("timeMillis must not be negative: " + timeMillis);
    }
    if (bytes < 0) {
      throw new IllegalArgumentException("bytes must not be negative: " + bytes);
    }
  }

  /**
   * Creates a new builder for {@link TranslatedStatisticEntry}.
   *
   * @return a new, empty builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for {@link TranslatedStatisticEntry}. */
  public static final class Builder {
    private @Nullable LocalDate period;
    private @Nullable String language;
    private long timeMillis;
    private long bytes;

    private Builder() {}

    /**
     * Sets the first day of the period.
     *
     * @param period the first day of the period
     * @return this builder
     */
    public Builder period(LocalDate period) {
      this.period = period;
      return this;
    }

    /**
     * Sets the target language.
     *
     * @param language the ISO 639 language code
     * @return this builder
     */
    public Builder language(String language) {
      this.language = language;
      return this;
    }

    /**
     * Sets the translation time.
     *
     * @param timeMillis the time in milliseconds
     * @return this builder
     */
    public Builder timeMillis(long timeMillis) {
      this.timeMillis = timeMillis;
      return this;
    }

    /**
     * Sets the size of the translated texts.
     *
     * @param bytes the size in bytes
     * @return this builder
     */
    public Builder bytes(long bytes) {
      this.bytes = bytes;
      return this;
    }

    /**
     * Builds the entry.
     *
     * @return the entry
     */
    @SuppressWarnings("NullAway")
    public TranslatedStatisticEntry build() {
      return new TranslatedStatisticEntry(period, language, timeMillis, bytes);
    }
  }
}
