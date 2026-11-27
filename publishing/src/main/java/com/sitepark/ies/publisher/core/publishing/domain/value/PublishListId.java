package com.sitepark.ies.publisher.core.publishing.domain.value;

import java.util.Objects;

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
   * @throws NullPointerException if {@code value} is {@code null}
   * @throws IllegalArgumentException if {@code value} is blank
   */
  public PublishListId {
    Objects.requireNonNull(value, "Id must not be null or blank");
    if (value.isBlank()) {
      throw new IllegalArgumentException("Id must not be null or blank");
    }
  }

  @Override
  public String toString() {
    return this.value;
  }
}
