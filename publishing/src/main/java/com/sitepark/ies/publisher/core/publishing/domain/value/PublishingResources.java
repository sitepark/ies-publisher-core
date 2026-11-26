package com.sitepark.ies.publisher.core.publishing.domain.value;

/**
 * What the server has to publish with, as far as it limits how much can be published at once.
 *
 * @param processors the processors available to the server, at least 1
 * @param connectionPoolSize the database connections the server may hold at the same time; for a
 *     server with several databases the smallest of their pools, at least 1
 */
public record PublishingResources(int processors, int connectionPoolSize) {

  /**
   * Validates that there is at least one processor and one connection.
   *
   * @throws IllegalArgumentException if {@code processors} or {@code connectionPoolSize} is below 1
   */
  public PublishingResources {
    if (processors <= 0) {
      throw new IllegalArgumentException("A server has at least one processor");
    }
    if (connectionPoolSize <= 0) {
      throw new IllegalArgumentException("A server has at least one database connection");
    }
  }
}
