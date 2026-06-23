package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Paginated result of a link check run, holding the checked items together with paging metadata.
 */
@JsonDeserialize(builder = LinkCheckerResult.Builder.class)
public final class LinkCheckerResult {

  private final List<LinkCheckerResultItem> items;

  private final int total;

  private final int start;

  private final int limit;

  /**
   * Creates a new result from the given builder.
   *
   * @param builder the builder holding the values to copy
   */
  public LinkCheckerResult(Builder builder) {
    this.items = Collections.unmodifiableList(builder.items);
    this.total = builder.total;
    this.start = builder.start;
    this.limit = builder.limit;
  }

  /**
   * Returns the checked link items of this result page.
   *
   * @return the immutable list of result items
   */
  public List<LinkCheckerResultItem> getItems() {
    return items;
  }

  /**
   * Returns the total number of matching items across all pages.
   *
   * @return the total item count
   */
  public int getTotal() {
    return total;
  }

  /**
   * Returns the zero-based offset of the first item in this page.
   *
   * @return the start offset
   */
  public int getStart() {
    return start;
  }

  /**
   * Returns the maximum number of items requested per page.
   *
   * @return the page limit
   */
  public int getLimit() {
    return limit;
  }

  /**
   * Creates a new builder for {@link LinkCheckerResult}.
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
    return Objects.hash(items, total, start, limit);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LinkCheckerResult that)) {
      return false;
    }
    return Objects.equals(this.items, that.items)
        && this.total == that.total
        && this.start == that.start
        && this.limit == that.limit;
  }

  @Override
  public String toString() {
    return "LinkCheckerResult [items="
        + items
        + ", total="
        + total
        + ", start="
        + start
        + ", limit="
        + limit
        + "]";
  }

  /** Builder for {@link LinkCheckerResult}. */
  @JsonPOJOBuilder(withPrefix = "")
  public static final class Builder {

    private final List<LinkCheckerResultItem> items = new ArrayList<>();

    private int total;

    private int start;

    private int limit;

    private Builder() {}

    private Builder(LinkCheckerResult instance) {
      this.items.addAll(instance.items);
      this.total = instance.total;
      this.start = instance.start;
      this.limit = instance.limit;
    }

    /**
     * Sets the items, replacing any previously added items.
     *
     * @param items the result items to set
     * @return this builder for chaining
     */
    public Builder items(List<LinkCheckerResultItem> items) {
      Objects.requireNonNull(items, "items must not be null");
      this.items.clear();
      for (LinkCheckerResultItem item : items) {
        this.item(item);
      }
      return this;
    }

    /**
     * Adds a single result item.
     *
     * @param item the result item to add
     * @return this builder for chaining
     */
    public Builder item(LinkCheckerResultItem item) {
      Objects.requireNonNull(item, "item must not be null");
      this.items.add(item);
      return this;
    }

    /**
     * Sets the total number of matching items across all pages.
     *
     * @param total the total item count
     * @return this builder for chaining
     */
    public Builder total(int total) {
      this.total = total;
      return this;
    }

    /**
     * Sets the zero-based offset of the first item in this page.
     *
     * @param start the start offset
     * @return this builder for chaining
     */
    public Builder start(int start) {
      this.start = start;
      return this;
    }

    /**
     * Sets the maximum number of items per page.
     *
     * @param limit the page limit
     * @return this builder for chaining
     */
    public Builder limit(int limit) {
      this.limit = limit;
      return this;
    }

    /**
     * Builds a new {@link LinkCheckerResult} instance from this builder.
     *
     * @return the built result
     */
    public LinkCheckerResult build() {
      return new LinkCheckerResult(this);
    }
  }
}
