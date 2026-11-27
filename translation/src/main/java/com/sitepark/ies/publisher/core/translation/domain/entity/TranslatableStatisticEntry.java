package com.sitepark.ies.publisher.core.translation.domain.entity;

import java.time.LocalDate;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Volume of texts of one publisher that would have been translated in one period.
 *
 * <p>The volume is recorded for every publisher that produces translatable texts, whether
 * translation is enabled for it or not. It is the basis for estimating the cost of enabling the
 * translation. Only texts that were not yet known for the published article are counted.
 *
 * @param period the first day of the period
 * @param publisherId the id of the publisher
 * @param publisherName the display name of the publisher, or {@code null} if unknown (e.g. the
 *     publisher was deleted)
 * @param bytes the size of the translatable source texts in bytes
 */
public record TranslatableStatisticEntry(
    LocalDate period, String publisherId, @Nullable String publisherName, long bytes) {

  /**
   * Validates the entry.
   *
   * @throws NullPointerException if {@code period} or {@code publisherId} is {@code null}
   * @throws IllegalArgumentException if {@code bytes} is negative
   */
  public TranslatableStatisticEntry {
    Objects.requireNonNull(period, "period is required");
    Objects.requireNonNull(publisherId, "publisherId is required");
    if (bytes < 0) {
      throw new IllegalArgumentException("bytes must not be negative: " + bytes);
    }
  }

  /**
   * Returns a copy of this entry with the given publisher name.
   *
   * @param publisherName the display name of the publisher, or {@code null} if unknown
   * @return the copy
   */
  public TranslatableStatisticEntry withPublisherName(@Nullable String publisherName) {
    return new TranslatableStatisticEntry(period, publisherId, publisherName, bytes);
  }

  /**
   * Creates a new builder for {@link TranslatableStatisticEntry}.
   *
   * @return a new, empty builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for {@link TranslatableStatisticEntry}. */
  @SuppressWarnings("NullAway.Init") // required fields are set after construction
  public static final class Builder {
    private LocalDate period;
    private String publisherId;
    private @Nullable String publisherName;
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
     * Sets the id of the publisher.
     *
     * @param publisherId the publisher id
     * @return this builder
     */
    public Builder publisherId(String publisherId) {
      this.publisherId = publisherId;
      return this;
    }

    /**
     * Sets the display name of the publisher.
     *
     * @param publisherName the display name, or {@code null} if unknown
     * @return this builder
     */
    public Builder publisherName(@Nullable String publisherName) {
      this.publisherName = publisherName;
      return this;
    }

    /**
     * Sets the size of the translatable texts.
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
    public TranslatableStatisticEntry build() {
      return new TranslatableStatisticEntry(period, publisherId, publisherName, bytes);
    }
  }
}
