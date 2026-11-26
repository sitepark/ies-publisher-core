package com.sitepark.ies.publisher.core.publishing.domain.value;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedUrlMapping;
import org.jspecify.annotations.Nullable;

/**
 * A URL mapping that was not stored because another owner holds its path in the site and channel.
 *
 * <p>A path leads to one owner. Who asks for it first keeps it; the other one is still reached by
 * its own URL and gets the path once the holder gives it up and the mappings are stored again.
 *
 * @param mapping the mapping that was not stored; must not be {@code null}
 * @param holderOwnerId the owner holding the path, or {@code null} if it gave the path up while
 *     this was found out
 */
public record UrlMappingConflict(PublishedUrlMapping mapping, @Nullable String holderOwnerId) {

  /**
   * Validates that the mapping is present.
   *
   * @throws IllegalArgumentException if {@code mapping} is {@code null}
   */
  public UrlMappingConflict {
    if (mapping == null) {
      throw new IllegalArgumentException("Mapping must not be null");
    }
  }
}
