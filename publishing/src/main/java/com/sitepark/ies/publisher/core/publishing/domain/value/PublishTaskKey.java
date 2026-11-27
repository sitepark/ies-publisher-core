package com.sitepark.ies.publisher.core.publishing.domain.value;

import java.util.Objects;

/**
 * Identifies what a publish task works on: an entity in a channel.
 *
 * <p>The key is the unit of mutual exclusion. Whatever is done for an entity in a channel -
 * publishing it, revoking it, cleaning up after it - never runs twice at the same time.
 *
 * @param channelId the channel; must not be {@code null} or blank
 * @param entityId the entity; must not be {@code null} or blank
 */
public record PublishTaskKey(String channelId, String entityId) {

  /**
   * Validates that channel and entity are present.
   *
   * @throws NullPointerException if {@code channelId} or {@code entityId} is {@code null}
   * @throws IllegalArgumentException if {@code channelId} or {@code entityId} is blank
   */
  public PublishTaskKey {
    Objects.requireNonNull(channelId, "Channel ID must not be null or blank");
    if (channelId.isBlank()) {
      throw new IllegalArgumentException("Channel ID must not be null or blank");
    }
    Objects.requireNonNull(entityId, "Entity ID must not be null or blank");
    if (entityId.isBlank()) {
      throw new IllegalArgumentException("Entity ID must not be null or blank");
    }
  }
}
