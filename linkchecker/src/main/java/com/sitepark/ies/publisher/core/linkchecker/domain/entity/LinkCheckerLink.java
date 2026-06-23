package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Objects;

/** External link to be checked, identified by a content hash and addressed by its URL. */
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

  /**
   * Returns the hash uniquely identifying this link.
   *
   * @return the link hash
   */
  public String getHash() {
    return this.hash;
  }

  /**
   * Returns the URL to be checked.
   *
   * @return the link URL
   */
  public String getUrl() {
    return this.url;
  }

  /**
   * Returns the request timeout in milliseconds used when checking this link.
   *
   * @return the timeout in milliseconds
   */
  public int getTimeout() {
    return this.timeout;
  }

  /**
   * Creates a new builder for {@link LinkCheckerLink}.
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
    return Objects.hash(this.hash, this.url, this.timeout);
  }

  @Override
  public boolean equals(Object o) {

    if (!(o instanceof LinkCheckerLink that)) {
      return false;
    }

    return Objects.equals(this.hash, that.hash)
        && Objects.equals(this.url, that.url)
        && this.timeout == that.timeout;
  }

  @Override
  public String toString() {
    return "LinkCheckerLink [hash=" + hash + ", url=" + url + ", timeout=" + timeout + "]";
  }

  /** Builder for {@link LinkCheckerLink}. */
  @JsonPOJOBuilder(withPrefix = "")
  @SuppressWarnings("NullAway.Init")
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

    /**
     * Sets the hash.
     *
     * @param hash the hash uniquely identifying the link
     * @return this builder for chaining
     */
    public Builder hash(String hash) {
      this.hash = hash;
      return this;
    }

    /**
     * Sets the URL.
     *
     * @param url the URL to be checked
     * @return this builder for chaining
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Sets the timeout.
     *
     * @param timeout the request timeout in milliseconds
     * @return this builder for chaining
     */
    public Builder timeout(int timeout) {
      this.timeout = timeout;
      return this;
    }

    /**
     * Builds a new {@link LinkCheckerLink} instance from this builder.
     *
     * @return the built instance
     */
    public LinkCheckerLink build() {
      return new LinkCheckerLink(this);
    }
  }
}
