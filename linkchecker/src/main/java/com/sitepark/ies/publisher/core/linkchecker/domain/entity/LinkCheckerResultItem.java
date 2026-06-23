package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Result of checking a single external link, including its status and the entities referencing it.
 */
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

  /**
   * Returns the checked URL.
   *
   * @return the URL
   */
  public String getUrl() {
    return this.url;
  }

  /**
   * Returns the hash identifying the checked link.
   *
   * @return the link hash
   */
  public String getHash() {
    return this.hash;
  }

  /**
   * Returns the status determined for the link.
   *
   * @return the status type
   */
  public StatusType getStatus() {
    return this.status;
  }

  /**
   * Returns the human-readable status message.
   *
   * @return the message
   */
  public String getMessage() {
    return this.message;
  }

  /**
   * Returns the identifiers of the entities referencing the link.
   *
   * @return the immutable list of entity identifiers
   */
  public List<String> getEntities() {
    return Collections.unmodifiableList(this.entities);
  }

  /**
   * Creates a new builder for {@link LinkCheckerResultItem}.
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

  /** Builder for {@link LinkCheckerResultItem}. */
  @JsonPOJOBuilder(withPrefix = "")
  @SuppressWarnings("NullAway.Init")
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

    /**
     * Sets the URL.
     *
     * @param url the checked URL
     * @return this builder for chaining
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Sets the hash identifying the checked link.
     *
     * @param hash the link hash
     * @return this builder for chaining
     */
    public Builder hash(String hash) {
      this.hash = hash;
      return this;
    }

    /**
     * Sets the status.
     *
     * @param status the status type
     * @return this builder for chaining
     */
    public Builder status(StatusType status) {
      this.status = status;
      return this;
    }

    /**
     * Sets the human-readable status message.
     *
     * @param message the message
     * @return this builder for chaining
     */
    public Builder message(String message) {
      this.message = message;
      return this;
    }

    /**
     * Sets the referencing entities, replacing any previously added entities.
     *
     * @param entities the entity identifiers to set
     * @return this builder for chaining
     */
    public Builder entities(Collection<String> entities) {
      Objects.requireNonNull(entities, "entities is null");
      this.entities.clear();
      for (String entity : entities) {
        this.entity(entity);
      }
      return this;
    }

    /**
     * Adds a single referencing entity identifier.
     *
     * @param entity the entity identifier to add
     * @return this builder for chaining
     */
    public Builder entity(String entity) {
      Objects.requireNonNull(entity, "entity is null");
      this.entities.add(entity);
      return this;
    }

    /**
     * Builds a new {@link LinkCheckerResultItem} instance from this builder.
     *
     * @return the built result item
     */
    public LinkCheckerResultItem build() {
      return new LinkCheckerResultItem(this);
    }
  }
}
