package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedFile;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileKey;
import com.sitepark.ies.publisher.core.publishing.port.PublishedFileRepository;
import jakarta.inject.Inject;
import java.util.Optional;

/**
 * Returns the file a channel has published for an entity or one of its media - what a link to the
 * entity or the medium resolves to.
 */
public class GetPublishedFile {

  private final PublishedFileRepository repository;

  /**
   * Creates the use case with its repository.
   *
   * @param repository the repository the published files are read from
   */
  @Inject
  public GetPublishedFile(PublishedFileRepository repository) {
    this.repository = repository;
  }

  /**
   * Returns the published file the key names.
   *
   * @param key the channel, entity and medium to look for
   * @return the published file, or empty if the channel holds none for the key - the usual answer
   *     for an entity that is not published in the channel
   */
  public Optional<PublishedFile> get(PublishedFileKey key) {
    return this.repository.find(key);
  }
}
