package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(builder = LinkCheckerResultItem.Builder.class)
public final class LinkCheckerResultItem {
  private final String url;
  private final String hash;
  private final StatusType status;
  private final String message;
  private final List<String> entities;

  private LinkCheckerResultItem(Builder builder) {
    this.url = builder.url;
    this.hash = builder.hash;
    this.status = builder.status;
    this.message = builder.message;
    this.entities = new ArrayList<>(builder.entities);
  }

  public String getUrl() {
    return this.url;
  }

  public String getHash() {
    return this.hash;
  }

  public StatusType getStatus() {
    return this.status;
  }

  public String getMessage() {
    return this.message;
  }

  public List<String> getEntities() {
    return Collections.unmodifiableList(this.entities);
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.url, this.hash, this.status, this.message, this.entities);
  }

  @Override
  public boolean equals(Object o) {

    if (!(o instanceof LinkCheckerResultItem that)) {
      return false;
    }

    return Objects.equals(this.url, that.url)
        && Objects.equals(this.hash, that.hash)
        && Objects.equals(this.status, that.status)
        && Objects.equals(this.message, that.message)
        && Objects.equals(this.entities, that.entities);
  }

  @Override
  public String toString() {
    return "LinkCheckerResultItem [url="
        + url
        + ", hash="
        + hash
        + ", status="
        + status
        + ", message="
        + message
        + ", entities="
        + entities
        + "]";
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static final class Builder {

    private String url;
    private String hash;
    private StatusType status;
    private String message;
    private final Collection<String> entities = new HashSet<>();

    private Builder() {}

    private Builder(LinkCheckerResultItem instance) {
      this.url = instance.url;
      this.hash = instance.hash;
      this.status = instance.status;
      this.message = instance.message;
      this.entities.addAll(instance.entities);
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder hash(String hash) {
      this.hash = hash;
      return this;
    }

    public Builder status(StatusType status) {
      this.status = status;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder entities(Collection<String> entities) {
      Objects.requireNonNull(entities, "entities is null");
      this.entities.clear();
      for (String entity : entities) {
        this.entity(entity);
      }
      return this;
    }

    public Builder entity(String entity) {
      Objects.requireNonNull(entity, "entity is null");
      this.entities.add(entity);
      return this;
    }

    public LinkCheckerResultItem build() {
      return new LinkCheckerResultItem(this);
    }
  }
}
