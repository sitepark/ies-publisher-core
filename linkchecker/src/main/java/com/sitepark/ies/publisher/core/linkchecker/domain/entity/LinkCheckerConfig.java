package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Configuration controlling whether, how and when the link checker runs. */
@JsonDeserialize(builder = LinkCheckerConfig.Builder.class)
public final class LinkCheckerConfig {

  private final boolean enabled;

  private final int parallel;

  private final int timeout;

  private final Scheduling scheduling;

  private final List<LinkCheckerExcludePattern> excludes;

  /** Minimum allowed number of links checked in parallel. */
  public static final int MIN_PARALLEL = 1;

  /** Maximum allowed number of links checked in parallel. */
  public static final int MAX_PARALLEL = 5;

  private LinkCheckerConfig(Builder builder) {
    this.enabled = builder.enabled;
    this.parallel = builder.parallel;
    this.timeout = builder.timeout;
    this.scheduling = builder.scheduling;
    this.excludes = builder.excludes;
  }

  /**
   * Returns whether the link checker is enabled.
   *
   * @return {@code true} if the link checker is enabled
   */
  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * Returns the number of links checked in parallel.
   *
   * @return the parallelism level
   */
  public int getParallel() {
    return this.parallel;
  }

  /**
   * Returns the request timeout in milliseconds used when checking a link.
   *
   * @return the timeout in milliseconds
   */
  public int getTimeout() {
    return this.timeout;
  }

  /**
   * Returns the scheduling strategy that triggers automatic checks.
   *
   * @return the scheduling strategy
   */
  public Scheduling getScheduling() {
    return this.scheduling;
  }

  /**
   * Returns the patterns excluding matching URLs from being checked.
   *
   * @return the exclude patterns
   */
  public List<LinkCheckerExcludePattern> getExcludes() {
    return Collections.unmodifiableList(this.excludes);
  }

  /**
   * Creates a new builder for {@link LinkCheckerConfig}.
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
    return Objects.hash(this.enabled, this.timeout, this.parallel, this.scheduling, this.excludes);
  }

  @Override
  public boolean equals(Object o) {

    if (!(o instanceof LinkCheckerConfig that)) {
      return false;
    }

    return this.enabled == that.enabled
        && this.timeout == that.timeout
        && this.parallel == that.parallel
        && Objects.equals(this.scheduling, that.scheduling)
        && Objects.equals(this.excludes, that.excludes);
  }

  @Override
  public String toString() {
    return "LinkCheckerConfig [enabled="
        + enabled
        + ", parallel="
        + parallel
        + ", timeout="
        + timeout
        + ", scheduling="
        + scheduling
        + ", excludes="
        + excludes
        + "]";
  }

  /** Builder for {@link LinkCheckerConfig}. */
  @JsonPOJOBuilder(withPrefix = "")
  @SuppressWarnings("NullAway.Init")
  public static class Builder {

    private boolean enabled;

    private int parallel;

    private int timeout;

    private Scheduling scheduling;

    private final List<LinkCheckerExcludePattern> excludes = new ArrayList<>();

    /** Creates an empty builder. */
    protected Builder() {}

    /**
     * Creates a builder pre-populated with the given instance's values.
     *
     * @param instance the configuration to copy values from
     */
    protected Builder(LinkCheckerConfig instance) {
      this.enabled = instance.enabled;
      this.parallel = instance.parallel;
      this.timeout = instance.timeout;
      this.excludes.addAll(instance.excludes);
      this.scheduling = instance.scheduling;
    }

    /**
     * Sets whether the link checker is enabled.
     *
     * @param enabled {@code true} to enable the link checker
     * @return this builder for chaining
     */
    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the scheduling strategy.
     *
     * @param scheduling the scheduling strategy triggering automatic checks
     * @return this builder for chaining
     */
    public Builder scheduling(Scheduling scheduling) {
      this.scheduling = scheduling;
      return this;
    }

    /**
     * Sets the parallelism level.
     *
     * @param parallel the number of links to check in parallel; must be between {@link
     *     #MIN_PARALLEL} and {@link #MAX_PARALLEL}
     * @return this builder for chaining
     * @throws IllegalArgumentException if {@code parallel} is out of range
     */
    public Builder parallel(int parallel) {
      if (parallel < MIN_PARALLEL) {
        throw new IllegalArgumentException("Parallel must be greater than 0");
      }
      if (parallel > MAX_PARALLEL) {
        throw new IllegalArgumentException("Parallel must not be greater than 5");
      }
      this.parallel = parallel;
      return this;
    }

    /**
     * Sets the request timeout.
     *
     * @param timeout the request timeout in milliseconds
     * @return this builder for chaining
     */
    public Builder timeout(int timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * Adds the given exclude patterns.
     *
     * @param excludes the patterns excluding matching URLs from being checked
     * @return this builder for chaining
     */
    public Builder excludes(List<LinkCheckerExcludePattern> excludes) {
      Objects.requireNonNull(excludes);
      for (LinkCheckerExcludePattern exclude : excludes) {
        this.exclude(exclude);
      }
      return this;
    }

    /**
     * Adds a single exclude pattern.
     *
     * @param exclude the pattern excluding matching URLs from being checked
     * @return this builder for chaining
     */
    public Builder exclude(LinkCheckerExcludePattern exclude) {
      Objects.requireNonNull(exclude);
      this.excludes.add(exclude);
      return this;
    }

    /**
     * Builds a new {@link LinkCheckerConfig} instance from this builder.
     *
     * @return the built instance
     */
    public LinkCheckerConfig build() {
      return new LinkCheckerConfig(this);
    }
  }
}
