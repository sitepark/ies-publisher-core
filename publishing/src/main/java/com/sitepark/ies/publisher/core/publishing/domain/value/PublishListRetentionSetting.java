package com.sitepark.ies.publisher.core.publishing.domain.value;

import java.time.Duration;

/**
 * How many lists that are over are kept to be looked at, and for how long, as an administrator set
 * it.
 *
 * @param keptLists how many lists are kept, the youngest ones; 0 keeps none
 * @param lifetimeHours how many hours a list is kept after it was over; 0 keeps none
 */
public record PublishListRetentionSetting(int keptLists, int lifetimeHours) {

  /** The most lists that can be kept. */
  public static final int MAX_KEPT_LISTS = 1000;

  /** The most hours a list can be kept: 31 days. */
  public static final int MAX_LIFETIME_HOURS = 24 * 31;

  /** What is kept when nobody set anything: 20 lists for 24 hours. */
  public static final PublishListRetentionSetting DEFAULT = new PublishListRetentionSetting(20, 24);

  /**
   * Validates the values.
   *
   * @throws IllegalArgumentException if a value is negative or above its maximum
   */
  public PublishListRetentionSetting {
    if (keptLists < 0 || keptLists > MAX_KEPT_LISTS) {
      throw new IllegalArgumentException(
          "The number of kept lists must be between 0 and " + MAX_KEPT_LISTS);
    }
    if (lifetimeHours < 0 || lifetimeHours > MAX_LIFETIME_HOURS) {
      throw new IllegalArgumentException(
          "The lifetime of a list must be between 0 and " + MAX_LIFETIME_HOURS + " hours");
    }
  }

  /**
   * The lifetime as a duration.
   *
   * @return the time a list is kept
   */
  public Duration lifetime() {
    return Duration.ofHours(this.lifetimeHours);
  }
}
