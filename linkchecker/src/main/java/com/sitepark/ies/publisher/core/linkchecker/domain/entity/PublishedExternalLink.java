package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Objects;

/** An external link as published by an entity within a specific channel and section. */
@JsonDeserialize(builder = PublishedExternalLink.Builder.class)
public final class PublishedExternalLink {

  private final String entity;

  private final String channel;

  private final String section;

  private final String url;

  private PublishedExternalLink(Builder builder) {
    this.entity = builder.entity;
    this.channel = builder.channel;
    this.section = builder.section;
    this.url = builder.url;
  }

  /**
   * Returns the identifier of the publishing entity.
   *
   * @return the entity identifier
   */
  public String getEntity() {
    return this.entity;
  }

  /**
   * Returns the channel the link was published in.
   *
   * @return the channel
   */
  public String getChannel() {
    return this.channel;
  }

  /**
   * Returns the section the link was published in.
   *
   * @return the section
   */
  public String getSection() {
    return this.section;
  }

  /**
   * Returns the published URL.
   *
   * @return the URL
   */
  public String getUrl() {
    return this.url;
  }

  /**
   * Creates a new builder for {@link PublishedExternalLink}.
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
    return Objects.hash(this.entity, this.channel, this.section, this.url);
  }

  @Override
  public boolean equals(Object o) {

    if (!(o instanceof PublishedExternalLink that)) {
      return false;
    }

    return Objects.equals(this.entity, that.entity)
        && Objects.equals(this.section, that.section)
        && Objects.equals(this.channel, that.channel)
        && Objects.equals(this.url, that.url);
  }

  @Override
  public String toString() {
    return "PublishedExternalLink [entity="
        + entity
        + ", channel="
        + channel
        + ", section="
        + section
        + ", url="
        + url
        + "]";
  }

  /** Builder for {@link PublishedExternalLink}. */
  @JsonPOJOBuilder(withPrefix = "")
  @SuppressWarnings("NullAway.Init")
  public static final class Builder {

    private String entity;

    private String channel;

    private String section;

    private String url;

    private Builder() {}

    private Builder(PublishedExternalLink instance) {
      this.entity = instance.entity;
      this.channel = instance.channel;
      this.section = instance.section;
      this.url = instance.url;
    }

    /**
     * Sets the publishing entity identifier.
     *
     * @param entity the entity identifier
     * @return this builder for chaining
     */
    public Builder entity(String entity) {
      this.entity = entity;
      return this;
    }

    /**
     * Sets the channel.
     *
     * @param channel the channel
     * @return this builder for chaining
     */
    public Builder channel(String channel) {
      this.channel = channel;
      return this;
    }

    /**
     * Sets the section.
     *
     * @param section the section
     * @return this builder for chaining
     */
    public Builder section(String section) {
      this.section = section;
      return this;
    }

    /**
     * Sets the URL.
     *
     * @param url the published URL
     * @return this builder for chaining
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Builds a new {@link PublishedExternalLink} instance from this builder.
     *
     * @return the built link
     */
    public PublishedExternalLink build() {
      return new PublishedExternalLink(this);
    }
  }
}
