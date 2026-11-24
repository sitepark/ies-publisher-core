package com.sitepark.ies.publisher.core.publishing.domain.entity;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFilePath;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileStatus;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedObjectFilesChanges;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The files an entity has published in a channel: its own file and the files of its media, each
 * possibly stored more than once - published, left stale by a generation cut short, or trashed and
 * waiting to be deleted.
 *
 * <p>Generating the entity publishes its files again. {@link #published(List)} answers what is
 * stored afterwards, {@link #changesTo(PublishedObjectFiles)} what to write for it:
 *
 * <ul>
 *   <li>a published file takes its place; any other file stored there is gone, since the file
 *       published last owns the place,
 *   <li>every other file of the entity is trashed: one its key was published to another place, or
 *       one the generation did not publish at all.
 * </ul>
 *
 * <p>This is what the legacy publisher did with an update of every file to stale before the
 * generation, a delete and an insert per generated file, and an update of what was left stale to
 * trashed afterwards - here computed once, so only the difference is written.
 *
 * @param channelId the channel; must not be {@code null} or blank
 * @param entityId the entity; must not be {@code null} or blank
 * @param files the stored files, each identified by key, type, path and status
 */
public record PublishedObjectFiles(
    String channelId, String entityId, List<PublishedFileEntry> files) {

  /** Of two files of the same key, the one further on its way to deletion goes first. */
  private static final Comparator<PublishedFileEntry> TRASHED_FIRST =
      Comparator.comparing(
          (PublishedFileEntry file) -> file.status() != PublishedFileStatus.TRASHED);

  /**
   * Validates the identifiers and that every file belongs to the entity in the channel.
   *
   * @throws IllegalArgumentException if {@code channelId} or {@code entityId} is {@code null} or
   *     blank, or a file belongs to another channel or entity
   */
  public PublishedObjectFiles {
    if (channelId == null || channelId.isBlank()) {
      throw new IllegalArgumentException("Channel ID must not be null or blank");
    }
    if (entityId == null || entityId.isBlank()) {
      throw new IllegalArgumentException("Entity ID must not be null or blank");
    }
    files = List.copyOf(files);
    for (PublishedFileEntry file : files) {
      requireOwn(channelId, entityId, file);
    }
  }

  private static void requireOwn(String channelId, String entityId, PublishedFileEntry file) {
    if (!channelId.equals(file.key().channelId()) || !entityId.equals(file.key().entityId())) {
      throw new IllegalArgumentException(
          "File " + file.key() + " does not belong to entity " + entityId + " in " + channelId);
    }
  }

  /**
   * Returns the file a link to the key resolves to: of the files stored for it, the one furthest
   * from deletion, a trashed one never.
   *
   * @param key the entity's own file or one of its media
   * @return the file, or empty if none is published for the key
   */
  public Optional<PublishedFileEntry> find(PublishedFileKey key) {
    return this.files.stream()
        .filter(file -> file.key().equals(key))
        .filter(file -> file.status() != PublishedFileStatus.TRASHED)
        .min(Comparator.comparing(PublishedFileEntry::status));
  }

  /**
   * Returns the files waiting to be deleted.
   *
   * @return the trashed files
   */
  public List<PublishedFileEntry> trashed() {
    return this.files.stream()
        .filter(file -> file.status() == PublishedFileStatus.TRASHED)
        .toList();
  }

  /**
   * Returns whether publishing the file creates a file or a URL the channel did not hold for its
   * key before - what makes the pages that link to it out of date.
   *
   * @param file a file of the entity
   * @return {@code true} unless a file of the same key, type, path and URL is stored, in any status
   */
  public boolean isNewOrMoved(PublishedFileEntry file) {
    return this.files.stream()
        .noneMatch(
            stored ->
                stored.key().equals(file.key())
                    && stored.type() == file.type()
                    && stored.file().equals(file.file())
                    && stored.url().equals(file.url()));
  }

  /**
   * Returns the files stored once the generation has published the given ones.
   *
   * @param published the files the generation published, in the order it published them; a key
   *     published twice ends up at the place it was published to last
   * @return the files of the entity afterwards
   * @throws IllegalArgumentException if a file belongs to another entity or channel
   */
  @SuppressWarnings("PMD.UseConcurrentHashMap") // local, keeps the order of the files
  public PublishedObjectFiles published(List<PublishedFileEntry> published) {
    Map<PublishedFileKey, PublishedFileEntry> last = new LinkedHashMap<>();
    List<PublishedFileEntry> replaced = new ArrayList<>();
    for (PublishedFileEntry file : published) {
      requireOwn(this.channelId, this.entityId, file);
      PublishedFileEntry earlier =
          last.put(file.key(), file.withStatus(PublishedFileStatus.PUBLISHED));
      if (earlier != null) {
        replaced.add(earlier);
      }
    }
    Set<PublishedFilePath> taken = new HashSet<>();
    for (PublishedFileEntry file : last.values()) {
      taken.add(file.path());
    }

    Map<Identity, PublishedFileEntry> after = new LinkedHashMap<>();
    for (PublishedFileEntry file : last.values()) {
      after.put(Identity.of(file), file);
    }
    List<PublishedFileEntry> left = new ArrayList<>(this.files);
    left.addAll(replaced);
    left.sort(TRASHED_FIRST);
    for (PublishedFileEntry file : left) {
      if (!taken.contains(file.path())) {
        PublishedFileEntry trashed = file.withStatus(PublishedFileStatus.TRASHED);
        after.putIfAbsent(Identity.of(trashed), trashed);
      }
    }
    return new PublishedObjectFiles(this.channelId, this.entityId, new ArrayList<>(after.values()));
  }

  /**
   * Returns what to write so that these stored files become the given ones.
   *
   * @param next the files of the entity afterwards, as {@link #published(List)} answers them
   * @return the changes, empty if nothing changed
   * @throws IllegalArgumentException if {@code next} belongs to another entity or channel
   */
  public PublishedObjectFilesChanges changesTo(PublishedObjectFiles next) {
    if (!this.channelId.equals(next.channelId) || !this.entityId.equals(next.entityId)) {
      throw new IllegalArgumentException("Files of another entity or channel");
    }
    Map<Identity, PublishedFileEntry> before = index(this.files);
    Map<Identity, PublishedFileEntry> after = index(next.files);
    Diff diff = new Diff();
    for (PublishedFileEntry file : before.values()) {
      diff.stored(file, before, after);
    }
    for (Map.Entry<Identity, PublishedFileEntry> file : after.entrySet()) {
      diff.wanted(file.getKey(), file.getValue(), before);
    }
    return new PublishedObjectFilesChanges(
        this.channelId,
        this.entityId,
        diff.inserts,
        diff.updates,
        diff.trashes,
        diff.deletes,
        this.claimedBy(next));
  }

  /** The places the next files take that none of these files holds as published. */
  private List<PublishedFilePath> claimedBy(PublishedObjectFiles next) {
    Set<PublishedFilePath> held = pathsOfPublished(this.files);
    return next.files.stream()
        .filter(file -> file.status() == PublishedFileStatus.PUBLISHED)
        .map(PublishedFileEntry::path)
        .filter(path -> !held.contains(path))
        .toList();
  }

  private static Set<PublishedFilePath> pathsOfPublished(List<PublishedFileEntry> files) {
    Set<PublishedFilePath> paths = new HashSet<>();
    for (PublishedFileEntry file : files) {
      if (file.status() == PublishedFileStatus.PUBLISHED) {
        paths.add(file.path());
      }
    }
    return paths;
  }

  /** Collects the writes, file by file. */
  private static final class Diff {

    private final List<PublishedFileEntry> inserts = new ArrayList<>();
    private final List<PublishedFileEntry> updates = new ArrayList<>();
    private final List<PublishedFileEntry> trashes = new ArrayList<>();
    private final List<PublishedFileEntry> deletes = new ArrayList<>();
    private final Set<Identity> movedToTrash = new HashSet<>();

    /** A stored file stays, changes in place, moves to the trash as it is, or goes. */
    void stored(
        PublishedFileEntry file,
        Map<Identity, PublishedFileEntry> before,
        Map<Identity, PublishedFileEntry> after) {
      PublishedFileEntry kept = after.get(Identity.of(file));
      if (kept != null) {
        if (!kept.equals(file)) {
          this.updates.add(kept);
        }
        return;
      }
      PublishedFileEntry trashed = file.withStatus(PublishedFileStatus.TRASHED);
      Identity identity = Identity.of(trashed);
      boolean trashedAsItIs = trashed.equals(after.get(identity)) && !before.containsKey(identity);
      if (trashedAsItIs && this.movedToTrash.add(identity)) {
        this.trashes.add(file);
      } else {
        this.deletes.add(file);
      }
    }

    /** A file afterwards that is not stored, and did not get there by moving to the trash. */
    void wanted(
        Identity identity, PublishedFileEntry file, Map<Identity, PublishedFileEntry> before) {
      if (!before.containsKey(identity) && !this.movedToTrash.contains(identity)) {
        this.inserts.add(file);
      }
    }
  }

  @SuppressWarnings("PMD.UseConcurrentHashMap") // local, keeps the order of the files
  private static Map<Identity, PublishedFileEntry> index(Collection<PublishedFileEntry> files) {
    Map<Identity, PublishedFileEntry> index = new LinkedHashMap<>();
    for (PublishedFileEntry file : files) {
      index.putIfAbsent(Identity.of(file), file);
    }
    return index;
  }

  /** What identifies a stored file; the rest of it can change in place. */
  private record Identity(
      PublishedFileKey key, PublishedFilePath path, PublishedFileStatus status) {
    static Identity of(PublishedFileEntry file) {
      return new Identity(file.key(), file.path(), file.status());
    }
  }
}
