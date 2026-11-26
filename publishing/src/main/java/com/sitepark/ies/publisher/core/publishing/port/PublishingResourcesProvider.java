package com.sitepark.ies.publisher.core.publishing.port;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishingResources;

/** Tells what the server has to publish with. */
@FunctionalInterface
public interface PublishingResourcesProvider {

  /**
   * Returns the processors and database connections of the server as they are now.
   *
   * @return the resources
   */
  PublishingResources get();
}
