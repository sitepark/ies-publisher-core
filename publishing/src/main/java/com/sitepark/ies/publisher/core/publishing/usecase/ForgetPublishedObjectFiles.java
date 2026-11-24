package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFilePath;
import com.sitepark.ies.publisher.core.publishing.port.PublishedObjectFilesCache;
import jakarta.inject.Inject;

/**
 * Tells that stored files changed outside this library - by the legacy publisher, which still
 * revokes entities, deletes trashed files and publishes previews - so they are read again.
 */
public class ForgetPublishedObjectFiles {

  private final PublishedObjectFilesCache cache;

  /**
   * Creates the use case with the cache to keep honest.
   *
   * @param cache the files read or written before
   */
  @Inject
  public ForgetPublishedObjectFiles(PublishedObjectFilesCache cache) {
    this.cache = cache;
  }

  /**
   * The files of an entity in a channel changed.
   *
   * @param channelId the channel
   * @param entityId the entity
   */
  public void entity(String channelId, String entityId) {
    this.cache.forget(channelId, entityId);
  }

  /**
   * The files at a place in a channel changed, whichever entity they belong to.
   *
   * @param channelId the channel
   * @param path the place
   */
  public void path(String channelId, PublishedFilePath path) {
    this.cache.forgetAt(channelId, path);
  }

  /**
   * Files of any entity in the channel changed.
   *
   * @param channelId the channel
   */
  public void channel(String channelId) {
    this.cache.forgetChannel(channelId);
  }
}
