package com.sitepark.ies.publisher.core.publishing.port;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedUrlMapping;
import java.util.List;

/** Persistence port for storing and retrieving {@link PublishedUrlMapping} entries. */
public interface PublishedUrlMappingRepository {

  /**
   * Returns all published URL mappings of the given channel.
   *
   * @param channelD the identifier of the channel to load mappings for
   * @return the mappings of the channel; never {@code null}
   */
  List<PublishedUrlMapping> getPublishedUrlMapping(String channelD);

  /**
   * Returns the published URL mappings of a single owner within the given channel.
   *
   * @param channelId the identifier of the channel to load mappings for
   * @param ownerId the identifier of the owning resource
   * @return the mappings of the owner within the channel; never {@code null}
   */
  List<PublishedUrlMapping> getPublishedUrlMappingByOwner(String channelId, String ownerId);

  /**
   * Returns the published URL mappings of a site within the given channel.
   *
   * <p>If a path is duplicated, the mapping with the highest priority is returned.
   *
   * @param channelId the identifier of the channel to load mappings for
   * @param siteId the identifier of the site resource
   * @return the mappings of the site within the channel; never {@code null}
   */
  List<PublishedUrlMapping> getPublishedUrlMappingBySite(String channelId, String siteId);

  /**
   * Persists the given mappings as new entries.
   *
   * @param mappings the mappings to create
   * @return the identifier assigned to the created entry
   */
  String create(List<PublishedUrlMapping> mappings);

  /**
   * Updates the given existing mappings.
   *
   * @param mappings the mappings to update
   */
  void update(List<PublishedUrlMapping> mappings);

  /**
   * Removes the mappings identified by the given identifiers.
   *
   * @param ids the identifiers of the mappings to remove
   * @return the number of mappings actually removed
   */
  int remove(List<String> ids);
}
