package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class LinkCheckerResult {

  private final List<LinkCheckerResultItem> items;

  private final int total;

  private final int start;

  private final int limit;

  public LinkCheckerResult(Builder builder) {
    this.items = Collections.unmodifiableList(builder.items);
    this.total = builder.total;
    this.start = builder.start;
    this.limit = builder.limit;
  }

  public List<LinkCheckerResultItem> getItems() {
    return items;
  }

  public int getTotal() {
    return total;
  }

  public int getStart() {
    return start;
  }

  public int getLimit() {
    return limit;
  }

  public static Builder builder() {
    return new Builder();
  }

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
        && Objects.equals(this.total, that.total)
        && Objects.equals(this.start, that.start)
        && Objects.equals(this.limit, that.limit);
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

  public static class Builder {

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

    public Builder items(List<LinkCheckerResultItem> items) {
      Objects.requireNonNull(items, "items must not be null");
      this.items.clear();
      for (LinkCheckerResultItem item : items) {
        this.items.add(item);
      }
      return this;
    }

    public Builder item(LinkCheckerResultItem item) {
      Objects.requireNonNull(item, "item must not be null");
      this.items.add(item);
      return this;
    }

    public Builder total(int total) {
      this.total = total;
      return this;
    }

    public Builder start(int start) {
      this.start = start;
      return this;
    }

    public Builder limit(int limit) {
      this.limit = limit;
      return this;
    }

    public LinkCheckerResult build() {
      return new LinkCheckerResult(this);
    }
  }
}
