package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Description of a link check to be run asynchronously as a background operation. */
public final class LinkCheckerBackgroundExecution {

  private final int parallel;

  private final String[] topic;

  private final List<LinkCheckerLink> links;

  private final Consumer<LinkCheckerLink> operation;

  private LinkCheckerBackgroundExecution(Builder builder) {
    this.parallel = builder.parallel;
    this.topic = builder.topic;
    this.links = Collections.unmodifiableList(builder.links);
    this.operation = builder.operation;
  }

  /**
   * Returns the number of links processed in parallel.
   *
   * @return the parallelism level
   */
  public int getParallel() {
    return this.parallel;
  }

  /**
   * Returns the hierarchical topic path under which this execution is grouped.
   *
   * @return a copy of the topic path
   */
  public String[] getTopic() {
    return this.topic.clone();
  }

  /**
   * Returns the links to be processed by this execution.
   *
   * @return the links to process
   */
  public List<LinkCheckerLink> getLinks() {
    return this.links;
  }

  /**
   * Returns the operation applied to each link.
   *
   * @return the per-link operation
   */
  public Consumer<LinkCheckerLink> getOperation() {
    return this.operation;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.parallel, Arrays.hashCode(this.topic), this.links, this.operation);
  }

  @Override
  public boolean equals(Object o) {

    if (!(o instanceof LinkCheckerBackgroundExecution that)) {
      return false;
    }

    return Arrays.equals(this.topic, that.topic)
        && this.parallel == that.parallel
        && Objects.equals(this.links, that.links)
        && Objects.equals(this.operation, that.operation);
  }

  @Override
  public String toString() {
    return "LinkCheckerBackgroundExecution [parallel="
        + parallel
        + ", topic="
        + Arrays.toString(topic)
        + ", links="
        + links
        + ", operation="
        + operation
        + "]";
  }

  /**
   * Creates a new builder for {@link LinkCheckerBackgroundExecution}.
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

  /** Builder for {@link LinkCheckerBackgroundExecution}. */
  @SuppressWarnings("NullAway.Init")
  public static final class Builder {

    private int parallel;

    private String[] topic;

    private final List<LinkCheckerLink> links = new ArrayList<>();

    private Consumer<LinkCheckerLink> operation;

    private Builder() {}

    private Builder(LinkCheckerBackgroundExecution instance) {
      this.parallel = instance.parallel;
      this.topic = instance.topic;
      this.links.addAll(instance.links);
      this.operation = instance.operation;
    }

    /**
     * Sets the parallelism level.
     *
     * @param parallel the number of links to process in parallel
     * @return this builder for chaining
     */
    public Builder parallel(int parallel) {
      this.parallel = parallel;
      return this;
    }

    /**
     * Sets the hierarchical topic path.
     *
     * <p>Topics are used to display all background operations for a specific topic. Topics are
     * hierarchical and the path of the topic is specified via a string array. Topics are freely
     * definable. If e.g. all Topics of <code>level1</code> are queried, all BackgroundExecutions
     * recursively below <code>level1</code> are returned.
     *
     * @param topic the hierarchical topic path; must not be {@code null} and must not contain
     *     {@code null} elements
     * @return this builder for chaining
     */
    public Builder topic(String... topic) {
      Objects.requireNonNull(topic, "topic is null");
      for (String part : topic) {
        Objects.requireNonNull(part, "operations contains null values");
      }

      this.topic = topic.clone();
      return this;
    }

    /**
     * Adds the given links to be processed.
     *
     * @param links the links to add
     * @return this builder for chaining
     */
    public Builder links(List<LinkCheckerLink> links) {
      Objects.requireNonNull(links, "links is null");
      for (LinkCheckerLink link : links) {
        this.link(link);
      }
      return this;
    }

    /**
     * Adds a single link to be processed.
     *
     * @param link the link to add
     * @return this builder for chaining
     */
    public Builder link(LinkCheckerLink link) {
      Objects.requireNonNull(link, "link is null");
      this.links.add(link);
      return this;
    }

    /**
     * Sets the operation applied to each link.
     *
     * @param operation the per-link operation
     * @return this builder for chaining
     */
    public Builder operation(Consumer<LinkCheckerLink> operation) {
      Objects.requireNonNull(operation, "operation is null");
      this.operation = operation;
      return this;
    }

    /**
     * Builds a new {@link LinkCheckerBackgroundExecution} instance from this builder.
     *
     * @return the built instance
     * @throws IllegalStateException if no topic has been set
     */
    public LinkCheckerBackgroundExecution build() {
      if (this.topic == null) {
        throw new IllegalStateException("topic must be set");
      }
      return new LinkCheckerBackgroundExecution(this);
    }
  }
}
