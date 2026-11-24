package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedFileEntry;
import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedObjectFiles;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFilePath;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedObjectFilesChanges;
import com.sitepark.ies.publisher.core.publishing.port.PublishedObjectFilesCache;
import com.sitepark.ies.publisher.core.publishing.port.PublishedObjectFilesRepository;
import jakarta.inject.Inject;
import java.util.List;

/**
 * Stores the files a generation of an entity published: they take their places, and the entity's
 * files it did not publish again are trashed. Only what changed is written, all at once.
 */
public class StorePublishedObjectFiles {

  private final PublishedObjectFilesRepository repository;

  private final PublishedObjectFilesCache cache;

  /**
   * Creates the use case with its repository and cache.
   *
   * @param repository the repository the files are written to
   * @param cache the files read or written before, kept up to date with what is written
   */
  @Inject
  public StorePublishedObjectFiles(
      PublishedObjectFilesRepository repository, PublishedObjectFilesCache cache) {
    this.repository = repository;
    this.cache = cache;
  }

  /**
   * Stores the files the generation published.
   *
   * @param stored the files of the entity when the generation started, as {@link
   *     GetPublishedObjectFiles} returned them
   * @param published the files the generation published, in the order it published them
   * @return the files of the entity afterwards; its {@linkplain PublishedObjectFiles#trashed()
   *     trashed} ones are the files to delete from the channel
   */
  public PublishedObjectFiles store(
      PublishedObjectFiles stored, List<PublishedFileEntry> published) {
    PublishedObjectFiles next = stored.published(published);
    PublishedObjectFilesChanges changes = stored.changesTo(next);
    if (!changes.isEmpty()) {
      this.repository.store(changes);
      // the files of other entities at a place taken now are gone from the table
      for (PublishedFilePath path : changes.claimedPaths()) {
        this.cache.forgetAt(next.channelId(), path);
      }
    }
    this.cache.put(next);
    return next;
  }
}
