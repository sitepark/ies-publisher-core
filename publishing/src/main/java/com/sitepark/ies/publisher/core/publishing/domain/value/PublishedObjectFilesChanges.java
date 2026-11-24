package com.sitepark.ies.publisher.core.publishing.domain.value;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedFileEntry;
import java.util.List;

/**
 * What to write so that the stored files of an entity in a channel become the ones its generation
 * published. Only what changed: a generation mostly publishes the files it published the last time.
 *
 * <p>A file is identified by its key, type, path and status; everything else about it can be updated
 * in place.
 *
 * @param channelId the channel; must not be {@code null}
 * @param entityId the entity; must not be {@code null}
 * @param inserts files to store
 * @param updates stored files to overwrite with these, identified as before
 * @param trashes stored files to move to {@link PublishedFileStatus#TRASHED}, as they are stored now
 * @param deletes stored files to delete, as they are stored now
 * @param claimedPaths places the entity takes for the first time: files of other entities that lie
 *     there are deleted, since the file published last owns the place
 */
public record PublishedObjectFilesChanges(
    String channelId,
    String entityId,
    List<PublishedFileEntry> inserts,
    List<PublishedFileEntry> updates,
    List<PublishedFileEntry> trashes,
    List<PublishedFileEntry> deletes,
    List<PublishedFilePath> claimedPaths) {

  /**
   * Validates the identifiers and copies the lists.
   *
   * @throws IllegalArgumentException if {@code channelId} or {@code entityId} is {@code null} or
   *     blank
   */
  public PublishedObjectFilesChanges {
    if (channelId == null || channelId.isBlank()) {
      throw new IllegalArgumentException("Channel ID must not be null or blank");
    }
    if (entityId == null || entityId.isBlank()) {
      throw new IllegalArgumentException("Entity ID must not be null or blank");
    }
    inserts = List.copyOf(inserts);
    updates = List.copyOf(updates);
    trashes = List.copyOf(trashes);
    deletes = List.copyOf(deletes);
    claimedPaths = List.copyOf(claimedPaths);
  }

  /**
   * Returns whether there is nothing to write.
   *
   * @return {@code true} if every list is empty
   */
  public boolean isEmpty() {
    return this.inserts.isEmpty()
        && this.updates.isEmpty()
        && this.trashes.isEmpty()
        && this.deletes.isEmpty()
        && this.claimedPaths.isEmpty();
  }
}
