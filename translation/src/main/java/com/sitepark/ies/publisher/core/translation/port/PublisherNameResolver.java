package com.sitepark.ies.publisher.core.translation.port;

import java.util.Optional;

/**
 * Resolves the display name of a publisher.
 *
 * <p>Implementations live outside this library in adapter modules.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface PublisherNameResolver {

  /**
   * Returns the display name of the publisher with the given id.
   *
   * @param publisherId the id of the publisher
   * @return the display name, or empty if the publisher no longer exists
   */
  Optional<String> getName(String publisherId);
}
