package com.sitepark.ies.publisher.core.publishing.domain.value;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedUrlMapping;
import java.util.List;
import java.util.Objects;

/**
 * Represents the complete set of published URL mappings that belong to a single owner within one
 * channel.
 *
 * <p>An "owner" is the source object a mapping belongs to – this may be an article, a category, a
 * system page or any other publishable resource. The owner is identified by {@code ownerId} and is
 * not necessarily identical to the target a mapping points to (e.g. a redirect may belong to one
 * article but point to an external URL).
 *
 * <p>This type is a value object that acts as an aggregate transfer object for publishing
 * operations. It guarantees the invariant that <em>all</em> contained mappings belong to the same
 * {@code ownerId} and the same {@code channelId}. Because the owner and channel are held once at
 * the root, the contained mappings form a consistent, self-describing unit.
 *
 * <p>An empty mapping list is explicitly valid and carries the meaning "this owner currently has no
 * URL mappings in this channel" – which a replace operation interprets as "remove all existing
 * mappings of this owner/channel".
 *
 * @param ownerId the identifier of the owning resource (article, category, system page, ...); must
 *     not be {@code null} or blank
 * @param channelId the delivery channel these mappings apply to; must not be {@code null} or blank
 * @param mappings the complete set of mappings for this owner/channel; must not be {@code null} but
 *     may be empty
 */
public record OwnerUrlMappings(
    String ownerId, String channelId, List<PublishedUrlMapping> mappings) {

  public OwnerUrlMappings {
    if (ownerId == null || ownerId.isBlank()) {
      throw new IllegalArgumentException("Owner ID must not be null or blank");
    }
    if (channelId == null || channelId.isBlank()) {
      throw new IllegalArgumentException("Channel ID must not be null or blank");
    }
    if (mappings == null) {
      throw new IllegalArgumentException("Mappings must not be null");
    }

    // Defensive copy -> immutable value object
    mappings = List.copyOf(mappings);

    // Enforce the aggregate invariant: every mapping must belong to this owner and channel.
    for (PublishedUrlMapping mapping : mappings) {
      if (!ownerId.equals(mapping.ownerId())) {
        throw new IllegalArgumentException(
            "Mapping for path '"
                + mapping.path()
                + "' belongs to owner '"
                + mapping.ownerId()
                + "' but was added to owner '"
                + ownerId
                + "'");
      }
      if (!channelId.equals(mapping.channelId())) {
        throw new IllegalArgumentException(
            "Mapping for path '"
                + mapping.path()
                + "' belongs to channel '"
                + mapping.channelId()
                + "' but was added to channel '"
                + channelId
                + "'");
      }
    }
  }

  /**
   * Returns {@code true} if this owner currently has no URL mappings in this channel. A replace
   * operation interprets this as "remove all existing mappings of this owner/channel".
   *
   * @return {@code true} if there are no mappings
   */
  public boolean isEmpty() {
    return mappings.isEmpty();
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(withPrefix = "")
  @SuppressWarnings("NullAway.Init")
  public static class Builder {
    private String ownerId;
    private String channelId;
    private List<PublishedUrlMapping> mappings = List.of();

    public Builder ownerId(String ownerId) {
      this.ownerId = ownerId;
      return this;
    }

    public Builder channelId(String channelId) {
      this.channelId = channelId;
      return this;
    }

    public Builder mappings(List<PublishedUrlMapping> mappings) {
      Objects.requireNonNull(mappings, "mappings must not be null");
      this.mappings = List.copyOf(mappings);
      return this;
    }

    public OwnerUrlMappings build() {
      return new OwnerUrlMappings(ownerId, channelId, mappings);
    }
  }
}
