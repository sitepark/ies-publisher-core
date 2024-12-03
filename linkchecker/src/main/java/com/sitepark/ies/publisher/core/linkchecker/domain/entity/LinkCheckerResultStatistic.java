package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class LinkCheckerResultStatistic {

  private final List<StatusTypeCount> statusCounts;

  private LinkCheckerResultStatistic(Builder builder) {
    this.statusCounts = Collections.unmodifiableList(builder.statusCounts);
  }

  public List<StatusTypeCount> getStatusCounts() {
    return this.statusCounts;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.statusCounts);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LinkCheckerResultStatistic that)) {
      return false;
    }
    return Objects.equals(this.statusCounts, that.statusCounts);
  }

  @Override
  public String toString() {
    return "LinkCheckerResultStatistic [statusCounts=" + statusCounts + "]";
  }

  @JsonPOJOBuilder(withPrefix = "", buildMethodName = "build")
  public static class Builder {

    private final List<StatusTypeCount> statusCounts = new ArrayList<>();

    private Builder() {}

    private Builder(LinkCheckerResultStatistic instance) {
      this.statusCounts.addAll(instance.statusCounts);
    }

    public Builder statusCounts(List<StatusTypeCount> statusCounts) {
      Objects.requireNonNull(statusCounts, "statusCounts is null");
      for (StatusTypeCount statusCount : statusCounts) {
        this.statusCount(statusCount);
      }
      return this;
    }

    public Builder statusCount(StatusTypeCount statusCounts) {
      Objects.requireNonNull(statusCounts, "statusCounts is null");
      this.statusCounts.add(statusCounts);
      return this;
    }

    public LinkCheckerResultStatistic build() {
      return new LinkCheckerResultStatistic(this);
    }
  }
}
