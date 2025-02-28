package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Objects;

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

  public String getEntity() {
    return this.entity;
  }

  public String getChannel() {
    return this.channel;
  }

  public String getSection() {
    return this.section;
  }

  public String getUrl() {
    return this.url;
  }

  public static Builder builder() {
    return new Builder();
  }

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

  @JsonPOJOBuilder(withPrefix = "")
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

    public Builder entity(String entity) {
      this.entity = entity;
      return this;
    }

    public Builder channel(String channel) {
      this.channel = channel;
      return this;
    }

    public Builder section(String section) {
      this.section = section;
      return this;
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public PublishedExternalLink build() {
      return new PublishedExternalLink(this);
    }
  }
}
