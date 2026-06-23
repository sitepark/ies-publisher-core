package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.LocalTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Scheduling strategy that runs the link checker once per day at a configured start time. */
@JsonDeserialize(builder = DailyScheduling.Builder.class)
public final class DailyScheduling implements Scheduling {

  private final @Nullable LocalTime startTime;

  private DailyScheduling(Builder builder) {
    this.startTime = builder.startTime;
  }

  @Override
  public String getType() {
    return "daily";
  }

  /**
   * Returns the time of day at which the daily check starts.
   *
   * @return the configured start time, or {@code null} if none is set
   */
  @JsonFormat(pattern = "HH:mm")
  public @Nullable LocalTime getStartTime() {
    return this.startTime;
  }

  /**
   * Creates a new builder for {@link DailyScheduling}.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder pre-populated with this instance's values.
   *
   * @return a builder initialized from this instance
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.startTime);
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof DailyScheduling that) && Objects.equals(this.startTime, that.startTime);
  }

  @Override
  public String toString() {
    return "DailyScheduling [startTime=" + startTime + "]";
  }

  /** Builder for {@link DailyScheduling}. */
  @JsonPOJOBuilder(withPrefix = "")
  public static final class Builder {

    private @Nullable LocalTime startTime;

    private Builder() {}

    private Builder(DailyScheduling instance) {
      this.startTime = instance.startTime;
    }

    /**
     * Sets the start time.
     *
     * @param startTime the time of day at which the daily check starts
     * @return this builder for chaining
     */
    public Builder startTime(LocalTime startTime) {
      this.startTime = startTime;
      return this;
    }

    /**
     * Sets the start time from an hour and minute.
     *
     * @param hour the hour of day (0-23)
     * @param minute the minute of hour (0-59)
     * @return this builder for chaining
     */
    public Builder startTime(int hour, int minute) {
      this.startTime = LocalTime.of(hour, minute);
      return this;
    }

    /**
     * Builds a new {@link DailyScheduling} instance from this builder.
     *
     * @return the built instance
     */
    public DailyScheduling build() {
      return new DailyScheduling(this);
    }
  }
}
