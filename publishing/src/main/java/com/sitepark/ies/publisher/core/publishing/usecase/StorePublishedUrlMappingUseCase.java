package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedUrlMapping;
import com.sitepark.ies.publisher.core.publishing.domain.value.OwnerUrlMappings;
import com.sitepark.ies.publisher.core.publishing.port.PublishedUrlMappingRepository;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Synchronises the persisted URL mappings of a single owner within one channel against a new
 * desired state.
 *
 * <p>The use case compares the incoming set of mappings with the mappings currently stored for the
 * same owner/channel pair. Mappings that are no longer present are deleted; mappings that do not
 * yet exist are created. There is intentionally no update operation: a logical change to an
 * existing mapping (e.g. a different {@code mode} or {@code priority}) is represented as the
 * deletion of the old entry and the creation of a new one.
 *
 * <p>Two mappings are considered identical – and therefore unchanged – when all content fields
 * ({@code siteId}, {@code channelId}, {@code path}, {@code ownerId}, {@code mode}, {@code
 * priority}) match. The {@code id} field is deliberately excluded from this comparison because
 * incoming mappings may arrive without a pre-assigned identifier.
 */
public class StorePublishedUrlMappingUseCase {

  private final PublishedUrlMappingRepository repository;

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

    List<PublishedUrlMapping> toAdd =
        incoming.stream().filter(n -> existing.stream().noneMatch(n::hasSameContent)).toList();

    List<PublishedUrlMapping> removed =
        existing.stream().filter(e -> incoming.stream().noneMatch(e::hasSameContent)).toList();

    List<PublishedUrlMapping> added = new ArrayList<>();
    for (PublishedUrlMapping mapping : toAdd) {
      String id = this.repository.create(List.of(mapping));
      added.add(mapping.toBuilder().id(id).build());
    }

    if (!removed.isEmpty()) {
      this.repository.remove(removed.stream().map(PublishedUrlMapping::id).toList());
    }

    return new StorePublishedUrlMappingResult(added, removed);
  }
}
