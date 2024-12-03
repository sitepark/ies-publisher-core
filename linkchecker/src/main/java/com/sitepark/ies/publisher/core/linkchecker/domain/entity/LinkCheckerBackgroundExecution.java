package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class LinkCheckerBackgroundExecution {

  private final int parallel;

  private final String[] topic;

  private final List<LinkCheckerLink> links;

  private final Consumer<LinkCheckerLink> operation;

  protected LinkCheckerBackgroundExecution(Builder builder) {
    this.parallel = builder.parallel;
    this.topic = builder.topic;
    this.links = Collections.unmodifiableList(builder.links);
    this.operation = builder.operation;
  }

  public int getParallel() {
    return this.parallel;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public String[] getTopic() {
    return this.topic.clone();
  }

  public List<LinkCheckerLink> getLinks() {
    return this.links;
  }

  public Consumer<LinkCheckerLink> getOperation() {
    return this.operation;
  }

  @Override
  public final int hashCode() {
    return Objects.hash(Arrays.hashCode(this.topic));
  }

  @Override
  public final boolean equals(Object o) {

    if (!(o instanceof LinkCheckerBackgroundExecution that)) {
      return false;
    }

    return Arrays.equals(this.topic, that.topic);
  }

  @Override
  public String toString() {
    StringBuilder b =
        new StringBuilder(100)
            .append("EntityBackgroundExecution[topic:")
            .append(Arrays.toString(this.topic))
            .append(']');
    return b.toString();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private int parallel;

    private String[] topic;

    private final List<LinkCheckerLink> links = new ArrayList<>();

    private Consumer<LinkCheckerLink> operation;

    private Builder() {}

    public Builder parallel(int parallel) {
      this.parallel = parallel;
      return this;
    }

    /**
     * Topics are used to display all background operations for a specific topic. Topics are
     * hierarchical and the path of the topic is specified via a string array. Topics are freely
     * definable. If e.g. all Topics of <code>level1</code> are queried, all BackgroundExecutions
     * recursively below <code>level1</code> are returned.
     */
    public Builder topic(String... topic) {
      Objects.requireNonNull(topic, "topic is null");
      for (String part : topic) {
        Objects.requireNonNull(part, "operations contains null values");
      }

      this.topic = topic.clone();
      return this;
    }

    public Builder links(List<LinkCheckerLink> links) {
      Objects.requireNonNull(links, "links is null");
      for (LinkCheckerLink link : links) {
        this.link(link);
      }
      return this;
    }

    public Builder link(LinkCheckerLink link) {
      Objects.requireNonNull(link, "link is null");
      this.links.add(link);
      return this;
    }

    public Builder operation(Consumer<LinkCheckerLink> operation) {
      Objects.requireNonNull(operation, "operation is null");
      this.operation = operation;
      return this;
    }

    public LinkCheckerBackgroundExecution build() {
      if (this.topic == null) {
        throw new IllegalStateException("topic must be set");
      }
      return new LinkCheckerBackgroundExecution(this);
    }
  }
}
