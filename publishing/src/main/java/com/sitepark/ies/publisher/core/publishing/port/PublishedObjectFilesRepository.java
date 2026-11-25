package com.sitepark.ies.publisher.core.publishing.port;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedObjectFiles;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedObjectFilesChanges;

/** The files an entity has published in a channel, read and written as a whole. */
public interface PublishedObjectFilesRepository {

  /**
   * Returns the files the entity has published in the channel, in every status.
   *
   * @param channelId the channel
   * @param entityId the entity
   * @return the files, empty if the entity has published none
   */
  PublishedObjectFiles find(String channelId, String entityId);

  /**
   * Writes the changes, all or nothing: readers see the files as they were before or as they are
   * after, never in between.
   *
   * @param changes what to write
   */
  void store(PublishedObjectFilesChanges changes);
}
