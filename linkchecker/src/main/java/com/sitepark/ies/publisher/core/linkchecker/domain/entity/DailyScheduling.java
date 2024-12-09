package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.LocalTime;
import java.util.Objects;

@JsonDeserialize(builder = DailyScheduling.Builder.class)
public final class DailyScheduling implements Scheduling {

  private final LocalTime startTime;

  protected DailyScheduling(Builder builder) {
    this.startTime = builder.startTime;
  }

  @Override
  public String getType() {
    return "daily";
  }

  @JsonFormat(pattern = "HH:mm")
  public LocalTime getStartTime() {
    return this.startTime;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.startTime);
  }

  @Override
  public boolean equals(Object o) {

    if (!(o instanceof DailyScheduling that)) {
      return false;
    }

    return Objects.equals(this.startTime, that.startTime);
  }

  @Override
  public String toString() {
    return "DailyScheduling [startTime=" + startTime + "]";
  }

  @JsonPOJOBuilder(withPrefix = "", buildMethodName = "build")
  public static class Builder {

    private LocalTime startTime;

    private Builder() {}

    private Builder(DailyScheduling instance) {
      this.startTime = instance.startTime;
    }

    public Builder startTime(LocalTime startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder startTime(int hour, int minute) {
      this.startTime = LocalTime.of(hour, minute);
      return this;
    }

    public DailyScheduling build() {
      return new DailyScheduling(this);
    }
  }
}
