package com.sitepark.ies.publisher.core.publishing.domain.value;

/**
 * Identifies a list of publish tasks. Unique on a server, also across its restarts, and sortable
 * by the time the list was requested.
 *
 * @param value the id; must not be {@code null} or blank
 */
public record PublishListId(String value) {

  /**
   * Validates that the id is present.
   *
   * @throws IllegalArgumentException if {@code value} is {@code null} or blank
   */
  public PublishListId {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Id must not be null or blank");
    }
  }

  @Override
  public String toString() {
    return this.value;
  }
}
