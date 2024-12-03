package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import java.util.Objects;

public class LinkCheckerLink {

  private final String hash;

  private final String url;

  private final int timeout;

  protected LinkCheckerLink(Builder builder) {
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
    return Objects.hash(this.hash, this.url);
  }

  @Override
  public boolean equals(Object o) {

    if (!(o instanceof LinkCheckerLink that)) {
      return false;
    }

    return Objects.equals(this.hash, that.hash) && Objects.equals(this.url, that.url);
  }

  public static class Builder {

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
