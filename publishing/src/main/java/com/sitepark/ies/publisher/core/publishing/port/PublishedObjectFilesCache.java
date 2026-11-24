package com.sitepark.ies.publisher.core.publishing.port;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedObjectFiles;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFilePath;
import java.util.Optional;

/**
 * Keeps the files of entities as they were last read or written, so a link to an entity's file is
 * answered without the database.
 *
 * <p>An entity is kept once its files were read or written, also when it has none. Whoever changes
 * the stored files outside the use cases of this library has to make the cache forget them.
 */
public interface PublishedObjectFilesCache {

  /**
   * Returns the files of the entity in the channel, if they are kept.
   *
   * @param channelId the channel
   * @param entityId the entity
   * @return the files, or empty if they are not kept
   */
  Optional<PublishedObjectFiles> get(String channelId, String entityId);

  /**
   * Keeps the files of an entity as they are stored now.
   *
   * @param files the files, of one entity in one channel
   */
  void put(PublishedObjectFiles files);

  /**
   * Forgets the files of an entity in a channel.
   *
   * @param channelId the channel
   * @param entityId the entity
   */
  void forget(String channelId, String entityId);

  /**
   * Forgets the files of every entity holding a file at the place in the channel.
   *
   * @param channelId the channel
   * @param path the place
   */
  void forgetAt(String channelId, PublishedFilePath path);

  /**
   * Forgets the files of every entity in the channel.
   *
   * @param channelId the channel
   */
  void forgetChannel(String channelId);
}
