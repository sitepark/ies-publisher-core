package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Objects;

@JsonDeserialize(builder = LinkCheckerLink.Builder.class)
public final class LinkCheckerLink {

  private final String hash;

  private final String url;

  private final int timeout;

  private LinkCheckerLink(Builder builder) {
    this.hash = builder.hash;
    this.url = builder.url;
    this.timeout = builder.timeout;
  }

  public String getHash() {
    return this.hash;
  }

  public String getUrl() {
    return this.url;
  }

  public int getTimeout() {
    return this.timeout;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.hash, this.url, this.timeout);
  }

  @Override
  public boolean equals(Object o) {

    if (!(o instanceof LinkCheckerLink that)) {
      return false;
    }

    return Objects.equals(this.hash, that.hash)
        && Objects.equals(this.url, that.url)
        && Objects.equals(this.timeout, that.timeout);
  }

  @Override
  public String toString() {
    return "LinkCheckerLink [hash=" + hash + ", url=" + url + ", timeout=" + timeout + "]";
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static final class Builder {

    private String hash;

    private String url;

    private int timeout;

    private Builder() {}

    private Builder(LinkCheckerLink instance) {
      this.hash = instance.hash;
      this.url = instance.url;
      this.timeout = instance.timeout;
    }

    public Builder hash(String hash) {
      this.hash = hash;
      return this;
    }

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder timeout(int timeout) {
      this.timeout = timeout;
      return this;
    }

    public LinkCheckerLink build() {
      return new LinkCheckerLink(this);
    }
  }
}
