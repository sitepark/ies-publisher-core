package com.sitepark.ies.publisher.core.publishing.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sitepark.ies.sharedkernel.domain.UrlMappingMode;
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Represents a single published URL mapping that links a path within a channel to its owning
 * resource and describes how the path should be resolved (e.g. redirect or forward).
 *
 * @param id the technical identifier assigned by the repository; {@code null} for a mapping that
 *     has not been persisted yet
 * @param siteId the identifier of the site the mapping belongs to; must not be {@code null} or
 *     blank
 * @param channelId the delivery channel the mapping applies to; must not be {@code null} or blank
 * @param path the URL path this mapping is registered for; must not be {@code null} or blank
 * @param ownerId the identifier of the owning resource (article, category, system page, ...); must
 *     not be {@code null} or blank
 * @param mode the resolution mode that defines how the path is handled; must not be {@code null}
 * @param priority the priority used to order competing mappings for the same path
 * @param locale the language the page should be requested in; {@code null} if the mapping is not
 *     language-specific
 */
public record PublishedUrlMapping(
    @Nullable String id,
    String siteId,
    String channelId,
    String path,
    String ownerId,
    UrlMappingMode mode,
    int priority,
    @Nullable Locale locale) {

  /**
   * Validates that all required fields are present and non-blank and that the mapping mode is set.
   */
  public PublishedUrlMapping {
    if (siteId == null || siteId.isBlank()) {
      throw new IllegalArgumentException("Site ID must not be null or blank");
    }
    if (channelId == null || channelId.isBlank()) {
      throw new IllegalArgumentException("Channel ID must not be null or blank");
    }
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("Path must not be null or blank");
    }
    if (ownerId == null || ownerId.isBlank()) {
      throw new IllegalArgumentException("Owner ID must not be null or blank");
    }
    if (mode == null) {
      throw new IllegalArgumentException("Mapping mode must not be null");
    }
  }

  /**
   * Returns whether this mapping resolves its path as a redirect.
   *
   * @return {@code true} if the mapping mode represents a redirect
   */
  public boolean isRedirect() {
    return mode.isRedirect();
  }

  /**
   * Returns whether this mapping resolves its path as a forward.
   *
   * @return {@code true} if the mapping mode represents a forward
   */
  public boolean isForward() {
    return mode.isForward();
  }

  @Override
  public String toString() {
    return "PublishedUrlMapping{"
        + "id='"
        + id
        + '\''
        + ", siteId='"
        + siteId
        + '\''
        + ", channelId='"
        + channelId
        + '\''
        + ", path='"
        + path
        + '\''
        + ", ownerId='"
        + ownerId
        + '\''
        + ", mode="
        + mode
        + ", priority="
        + priority
        + ", locale="
        + locale
        + '}';
  }

  /**
   * Returns {@code true} if this mapping is content-identical to {@code other}, ignoring the
   * technical {@code id} field. Two mappings are considered identical when {@code siteId}, {@code
   * channelId}, {@code path}, {@code ownerId}, {@code mode}, {@code priority}, and {@code locale}
   * all match.
   *
   * @param other the mapping to compare against; must not be {@code null}
   * @return {@code true} if all content fields are equal
   */
  public boolean hasSameContent(PublishedUrlMapping other) {
    return this.siteId.equals(other.siteId)
        && this.channelId.equals(other.channelId)
        && this.path.equals(other.path)
        && this.ownerId.equals(other.ownerId)
        && this.mode == other.mode
        && this.priority == other.priority
        && Objects.equals(this.locale, other.locale);
  }

  /**
   * Creates a new builder for {@link PublishedUrlMapping}.
   *
   * @return a new, empty builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder pre-populated with the values of this mapping.
   *
   * @return a builder initialized from this instance
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  /** Builder for {@link PublishedUrlMapping} instances. */
  @JsonPOJOBuilder(withPrefix = "")
  @SuppressWarnings("NullAway.Init")
  public static class Builder {
    private @Nullable String id;
    private String siteId;
    private String channelId;
    private String path;
    private String ownerId;
    private UrlMappingMode mode;
    private int priority;
    private @Nullable Locale locale;

    /** Creates a new, empty builder. */
    public Builder() {}

    private Builder(PublishedUrlMapping mapping) {
      this.id = mapping.id;
      this.siteId = mapping.siteId;
      this.channelId = mapping.channelId;
      this.path = mapping.path;
      this.ownerId = mapping.ownerId;
      this.mode = mapping.mode;
      this.priority = mapping.priority;
      this.locale = mapping.locale;
    }

    /**
     * Sets the technical identifier.
     *
     * @param id the repository-assigned identifier, or {@code null} if not yet persisted
     * @return this builder for chaining
     */
    public Builder id(@Nullable String id) {
      this.id = id;
      return this;
    }

    /**
     * Sets the site identifier.
     *
     * @param siteId the identifier of the site the mapping belongs to
     * @return this builder for chaining
     */
    public Builder siteId(String siteId) {
      this.siteId = siteId;
      return this;
    }

    /**
     * Sets the channel identifier.
     *
     * @param channelId the delivery channel the mapping applies to
     * @return this builder for chaining
     */
    public Builder channelId(String channelId) {
      this.channelId = channelId;
      return this;
    }

    /**
     * Sets the URL path.
     *
     * @param path the URL path this mapping is registered for
     * @return this builder for chaining
     */
    public Builder path(String path) {
      this.path = path;
      return this;
    }

    /**
     * Sets the owner identifier.
     *
     * @param ownerId the identifier of the owning resource
     * @return this builder for chaining
     */
    public Builder ownerId(String ownerId) {
      this.ownerId = ownerId;
      return this;
    }

    /**
     * Sets the resolution mode.
     *
     * @param mode the mapping mode that defines how the path is handled
     * @return this builder for chaining
     */
    public Builder mode(UrlMappingMode mode) {
      this.mode = mode;
      return this;
    }

    /**
     * Sets the priority.
     *
     * @param priority the priority used to order competing mappings for the same path
     * @return this builder for chaining
     */
    public Builder priority(int priority) {
      this.priority = priority;
      return this;
    }

    /**
     * Sets the language the page should be requested in.
     *
     * @param locale the language, or {@code null} if the mapping is not language-specific
     * @return this builder for chaining
     */
    public Builder locale(@Nullable Locale locale) {
      this.locale = locale;
      return this;
    }

    /**
     * Builds a new {@link PublishedUrlMapping} instance from this builder.
     *
     * @return a new, validated mapping
     */
    public PublishedUrlMapping build() {
      return new PublishedUrlMapping(id, siteId, channelId, path, ownerId, mode, priority, locale);
    }
  }
}
