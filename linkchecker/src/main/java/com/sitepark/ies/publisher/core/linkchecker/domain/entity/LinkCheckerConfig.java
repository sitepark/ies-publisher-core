package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(builder = LinkCheckerConfig.Builder.class)
public final class LinkCheckerConfig {

  private final boolean enabled;

  private final int parallel;

  private final int timeout;

  private final Scheduling scheduling;

  private final List<LinkCheckerExcludePattern> excludes;

  public static final int MIN_PARALLEL = 1;

  public static final int MAX_PARALLEL = 5;

  private LinkCheckerConfig(Builder builder) {
    this.enabled = builder.enabled;
    this.parallel = builder.parallel;
    this.timeout = builder.timeout;
    this.scheduling = builder.scheduling;
    this.excludes = builder.excludes;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public int getParallel() {
    return this.parallel;
  }

  public int getTimeout() {
    return this.timeout;
  }

  public Scheduling getScheduling() {
    return this.scheduling;
  }

  public List<LinkCheckerExcludePattern> getExcludes() {
    return Collections.unmodifiableList(this.excludes);
  }

  public static Builder builder() {
    return new Builder();
  }

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

    return Objects.equals(this.enabled, that.enabled)
        && Objects.equals(this.timeout, that.timeout)
        && Objects.equals(this.parallel, that.parallel)
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

  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder {

    private boolean enabled;

    private int parallel;

    private int timeout;

    public Scheduling scheduling;

    private final List<LinkCheckerExcludePattern> excludes = new ArrayList<>();

    protected Builder() {}

    protected Builder(LinkCheckerConfig instance) {
      this.enabled = instance.enabled;
      this.parallel = instance.parallel;
      this.timeout = instance.timeout;
      this.excludes.addAll(instance.excludes);
      this.scheduling = instance.scheduling;
    }

    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder scheduling(Scheduling scheduling) {
      this.scheduling = scheduling;
      return this;
    }

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

    public Builder timeout(int timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder excludes(List<LinkCheckerExcludePattern> excludes) {
      Objects.requireNonNull(excludes);
      for (LinkCheckerExcludePattern exclude : excludes) {
        this.exclude(exclude);
      }
      return this;
    }

    public Builder exclude(LinkCheckerExcludePattern exclude) {
      Objects.requireNonNull(exclude);
      this.excludes.add(exclude);
      return this;
    }

    public LinkCheckerConfig build() {
      return new LinkCheckerConfig(this);
    }
  }
}
