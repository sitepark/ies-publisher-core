package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedUrlMapping;
import com.sitepark.ies.publisher.core.publishing.domain.value.OwnerUrlMappings;
import com.sitepark.ies.publisher.core.publishing.domain.value.UrlMappingConflict;
import com.sitepark.ies.publisher.core.publishing.port.PublishedUrlMappingRepository;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Synchronises the persisted URL mappings of a single owner within one channel against a new
 * desired state.
 *
 * <p>The use case compares the incoming set of mappings with the mappings currently stored for the
 * same owner/channel pair. Mappings that are no longer present are deleted; mappings that do not
 * yet exist are created. A mapping that changes but keeps its path (e.g. a different {@code mode}
 * or {@code priority}) is updated in place and keeps its id, so the path is never free in between
 * and no other owner can take it. In the result it still shows as the old mapping removed and the
 * new one added.
 *
 * <p>A path leads to one owner in a site and channel. A mapping whose path another owner holds is
 * not stored and reported as a conflict instead of failing: the owner is still reached by its own
 * URL, and whoever stores the mappings can follow the holder to try again when it gives the path
 * up. Of two owners asking for a free path at the same time, exactly one gets it.
 *
 * <p>Two mappings are considered identical – and therefore unchanged – when all content fields
 * ({@code siteId}, {@code channelId}, {@code path}, {@code ownerId}, {@code mode}, {@code
 * priority}, {@code locale}) match. The {@code id} field is deliberately excluded from this comparison because
 * incoming mappings may arrive without a pre-assigned identifier.
 */
public class StorePublishedUrlMappingUseCase {

  private final PublishedUrlMappingRepository repository;

  /**
   * Creates the use case with its required repository dependency.
   *
   * @param repository the repository used to load and persist URL mappings
   */
  @Inject
  public StorePublishedUrlMappingUseCase(PublishedUrlMappingRepository repository) {
    this.repository = repository;
  }

  /**
   * Stores the given owner URL mappings by synchronising them with the current database state.
   *
   * <p>The method loads the existing mappings for the owner/channel combination from the
   * repository, computes the diff against {@code ownerUrlMappings}, deletes obsolete entries, and
   * creates new ones. Passing an {@link OwnerUrlMappings} instance with an empty mapping list
   * removes all existing mappings for that owner/channel.
   *
   * @param ownerUrlMappings the complete desired set of URL mappings for one owner within one
   *     channel; must not be {@code null}
   * @return a {@link StorePublishedUrlMappingResult} describing which mappings were added and which
   *     were removed; the added mappings carry the identifiers assigned during creation. {@link
   *     StorePublishedUrlMappingResult#isEmpty()} returns {@code true} if nothing changed
   */
  public StorePublishedUrlMappingResult storePublishedUrlMapping(
      OwnerUrlMappings ownerUrlMappings) {
    List<PublishedUrlMapping> existing =
        this.repository.getPublishedUrlMappingByOwner(
            ownerUrlMappings.channelId(), ownerUrlMappings.ownerId());
    List<PublishedUrlMapping> incoming = ownerUrlMappings.mappings();

    List<PublishedUrlMapping> toAdd = new ArrayList<>();
    for (PublishedUrlMapping mapping : incoming) {
      // a mapping asked for twice is stored once, and does not conflict with itself
      boolean known =
          existing.stream().anyMatch(mapping::hasSameContent)
              || toAdd.stream().anyMatch(mapping::hasSameContent);
      if (!known) {
        toAdd.add(mapping);
      }
    }

    List<PublishedUrlMapping> removed =
        existing.stream().filter(e -> incoming.stream().noneMatch(e::hasSameContent)).toList();

    // A mapping that keeps its path replaces the old entry in place: removing it first would free
    // the path for a moment, and another owner could take it.
    List<PublishedUrlMapping> toRemove = new ArrayList<>(removed);
    List<PublishedUrlMapping> toCreate = new ArrayList<>();
    List<PublishedUrlMapping> toUpdate = new ArrayList<>();
    for (PublishedUrlMapping mapping : toAdd) {
      Optional<PublishedUrlMapping> samePath =
          toRemove.stream().filter(old -> hasSamePath(old, mapping)).findFirst();
      if (samePath.isPresent()) {
        toRemove.remove(samePath.get());
        toUpdate.add(mapping.toBuilder().id(samePath.get().id()).build());
      } else {
        toCreate.add(mapping);
      }
    }

    if (!toRemove.isEmpty()) {
      this.repository.remove(toRemove.stream().map(PublishedUrlMapping::id).toList());
    }
    if (!toUpdate.isEmpty()) {
      this.repository.update(toUpdate);
    }

    List<PublishedUrlMapping> added = new ArrayList<>(toUpdate);
    List<UrlMappingConflict> conflicts = new ArrayList<>();
    for (PublishedUrlMapping mapping : toCreate) {
      Optional<String> id = this.repository.createIfFree(mapping);
      if (id.isPresent()) {
        added.add(mapping.toBuilder().id(id.get()).build());
      } else {
        conflicts.add(this.conflictOf(mapping));
      }
    }

    return new StorePublishedUrlMappingResult(added, removed, conflicts);
  }

  private static boolean hasSamePath(PublishedUrlMapping a, PublishedUrlMapping b) {
    return a.siteId().equals(b.siteId()) && a.path().equals(b.path());
  }

  /** The path is taken: by whom, so the owner can follow when the holder gives it up. */
  private UrlMappingConflict conflictOf(PublishedUrlMapping mapping) {
    String holder =
        this.repository
            .getPublishedUrlMappingByPath(mapping.channelId(), mapping.siteId(), mapping.path())
            .map(PublishedUrlMapping::ownerId)
            .orElse(null);
    return new UrlMappingConflict(mapping, holder);
  }
}
