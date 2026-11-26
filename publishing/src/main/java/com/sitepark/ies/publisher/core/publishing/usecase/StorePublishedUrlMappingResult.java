package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedUrlMapping;
import com.sitepark.ies.publisher.core.publishing.domain.value.UrlMappingConflict;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a URL mapping synchronisation operation.
 *
 * <p>Holds the two sets that describe what changed: mappings that were newly {@link #added} to the
 * repository and mappings that were {@link #removed} from it. Both lists are immutable and may be
 * empty; an instance where both are empty signals that the synchronisation found no difference
 * between the desired and the persisted state.
 *
 * @param added mappings that did not exist before and were created; never {@code null}
 * @param removed mappings that no longer exist and were deleted; never {@code null}
 * @param conflicts mappings that were not stored because another owner holds their path; never
 *     {@code null}. They are no change: {@link #isEmpty()} ignores them
 */
public record StorePublishedUrlMappingResult(
    List<PublishedUrlMapping> added,
    List<PublishedUrlMapping> removed,
    List<UrlMappingConflict> conflicts) {

  /**
   * A result without conflicts.
   *
   * @param added the mappings that were stored
   * @param removed the mappings that were deleted
   */
  public StorePublishedUrlMappingResult(
      List<PublishedUrlMapping> added, List<PublishedUrlMapping> removed) {
    this(added, removed, List.of());
  }

  /**
   * Validates that both lists are non-{@code null} and stores immutable copies of them.
   */
  public StorePublishedUrlMappingResult {
    Objects.requireNonNull(added, "added must not be null");
    Objects.requireNonNull(removed, "removed must not be null");
    added = List.copyOf(added);
    removed = List.copyOf(removed);
    Objects.requireNonNull(conflicts, "conflicts must not be null");
    conflicts = List.copyOf(conflicts);
  }

  /**
   * Returns {@code true} if neither list contains any entry, i.e. the synchronisation found nothing
   * to do.
   *
   * @return {@code true} when both {@code added} and {@code removed} are empty
   */
  public boolean isEmpty() {
    return this.added.isEmpty() && this.removed.isEmpty();
  }
}
