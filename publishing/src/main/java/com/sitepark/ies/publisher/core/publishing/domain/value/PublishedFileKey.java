package com.sitepark.ies.publisher.core.publishing.domain.value;

import org.jspecify.annotations.Nullable;

/**
 * Identifies the published file of an entity, or of one of its media, in a channel.
 *
 * <p>An entity publishes one file of its own and one file per medium it holds; a medium is
 * identified within its entity. All identifiers are the short ones, without the signature of the
 * client the entity belongs to.
 *
 * @param channelId the channel the file is published in; must not be {@code null} or blank
 * @param entityId the entity the file belongs to; must not be {@code null} or blank
 * @param mediaId the medium of the entity the file is published for, or {@code null} for the
 *     entity's own file; must not be blank
 */
public record PublishedFileKey(String channelId, String entityId, @Nullable String mediaId) {

  /**
   * Validates that channel and entity are present and that a medium, if given, is not blank.
   *
   * @throws IllegalArgumentException if {@code channelId} or {@code entityId} is {@code null} or
   *     blank, or {@code mediaId} is blank
   */
  public PublishedFileKey {
    if (channelId == null || channelId.isBlank()) {
      throw new IllegalArgumentException("Channel ID must not be null or blank");
    }
    if (entityId == null || entityId.isBlank()) {
      throw new IllegalArgumentException("Entity ID must not be null or blank");
    }
    if (mediaId != null && mediaId.isBlank()) {
      throw new IllegalArgumentException("Media ID must not be blank");
    }
  }

  /**
   * The key of an entity's own file.
   *
   * @param channelId the channel the file is published in
   * @param entityId the entity the file belongs to
   * @return the key
   */
  public static PublishedFileKey ofEntity(String channelId, String entityId) {
    return new PublishedFileKey(channelId, entityId, null);
  }

  /**
   * The key of the file a medium of an entity is published as.
   *
   * @param channelId the channel the file is published in
   * @param entityId the entity the medium belongs to
   * @param mediaId the medium
   * @return the key
   */
  public static PublishedFileKey ofMedia(String channelId, String entityId, String mediaId) {
    return new PublishedFileKey(channelId, entityId, mediaId);
  }

  /**
   * Returns whether the key names the file of a medium rather than the entity's own file.
   *
   * @return {@code true} if a medium is given
   */
  public boolean isMedia() {
    return this.mediaId != null;
  }
}
