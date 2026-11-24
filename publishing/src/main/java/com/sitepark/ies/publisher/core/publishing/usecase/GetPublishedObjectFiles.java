package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedObjectFiles;
import com.sitepark.ies.publisher.core.publishing.port.PublishedObjectFilesCache;
import com.sitepark.ies.publisher.core.publishing.port.PublishedObjectFilesRepository;
import jakarta.inject.Inject;
import java.util.Optional;

/**
 * Returns the files an entity has published in a channel - what a generation of the entity starts
 * from, and what a link to the entity or one of its media resolves to.
 */
public class GetPublishedObjectFiles {

  private final PublishedObjectFilesRepository repository;

  private final PublishedObjectFilesCache cache;

  /**
   * Creates the use case with its repository and cache.
   *
   * @param repository the repository the files are read from
   * @param cache the files read or written before
   */
  @Inject
  public GetPublishedObjectFiles(
      PublishedObjectFilesRepository repository, PublishedObjectFilesCache cache) {
    this.repository = repository;
    this.cache = cache;
  }

  /**
   * Returns the files the entity has published in the channel.
   *
   * @param channelId the channel
   * @param entityId the entity
   * @return the files in every status, empty if the entity has published none
   */
  public PublishedObjectFiles get(String channelId, String entityId) {
    Optional<PublishedObjectFiles> kept = this.cache.get(channelId, entityId);
    if (kept.isPresent()) {
      return kept.get();
    }
    // An entity without files is kept too: most links lead to entities a channel holds no file of.
    // Whoever stores files outside this library makes the cache forget them.
    PublishedObjectFiles files = this.repository.find(channelId, entityId);
    this.cache.put(files);
    return files;
  }
}
