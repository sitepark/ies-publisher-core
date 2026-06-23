package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Aggregated statistic of a link check run, holding the number of links per status type. */
@JsonDeserialize(builder = LinkCheckerResultStatistic.Builder.class)
public final class LinkCheckerResultStatistic {

  private final List<StatusTypeCount> statusCounts;

  private LinkCheckerResultStatistic(Builder builder) {
    this.statusCounts = Collections.unmodifiableList(builder.statusCounts);
  }

  /**
   * Returns the per-status-type counts.
   *
   * @return the immutable list of status type counts
   */
  public List<StatusTypeCount> getStatusCounts() {
    return this.statusCounts;
  }

  /**
   * Creates a new builder for {@link LinkCheckerResultStatistic}.
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
    return Objects.hash(this.statusCounts);
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof LinkCheckerResultStatistic that)
        && Objects.equals(this.statusCounts, that.statusCounts);
  }

  @Override
  public String toString() {
    return "LinkCheckerResultStatistic [statusCounts=" + statusCounts + "]";
  }

  /** Builder for {@link LinkCheckerResultStatistic}. */
  @JsonPOJOBuilder(withPrefix = "")
  public static final class Builder {

    private final List<StatusTypeCount> statusCounts = new ArrayList<>();

    private Builder() {}

    private Builder(LinkCheckerResultStatistic instance) {
      this.statusCounts.addAll(instance.statusCounts);
    }

    /**
     * Adds all given status type counts.
     *
     * @param statusCounts the status type counts to add
     * @return this builder for chaining
     */
    public Builder statusCounts(List<StatusTypeCount> statusCounts) {
      Objects.requireNonNull(statusCounts, "statusCounts is null");
      for (StatusTypeCount statusCount : statusCounts) {
        this.statusCount(statusCount);
      }
      return this;
    }

    /**
     * Adds a single status type count.
     *
     * @param statusCounts the status type count to add
     * @return this builder for chaining
     */
    public Builder statusCount(StatusTypeCount statusCounts) {
      Objects.requireNonNull(statusCounts, "statusCounts is null");
      this.statusCounts.add(statusCounts);
      return this;
    }

    /**
     * Builds a new {@link LinkCheckerResultStatistic} instance from this builder.
     *
     * @return the built statistic
     */
    public LinkCheckerResultStatistic build() {
      return new LinkCheckerResultStatistic(this);
    }
  }
}
