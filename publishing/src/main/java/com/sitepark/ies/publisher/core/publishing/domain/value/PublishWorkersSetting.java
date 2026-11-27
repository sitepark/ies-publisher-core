package com.sitepark.ies.publisher.core.publishing.domain.value;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * How many workers publish in the background, as an administrator set it: a number of their own,
 * or left to the server to work out from what it has.
 *
 * @param mode whether the number is worked out or given; must not be {@code null}
 * @param workers the number given, at least 1; {@code null} if it is worked out
 */
public record PublishWorkersSetting(Mode mode, @Nullable Integer workers) {

  /** Whether the number of workers is worked out or given. */
  public enum Mode {
    /** The server works the number out from its processors and its database connections. */
    AUTO,
    /** An administrator gave the number. */
    FIXED
  }

  /**
   * Validates that a given number is present and at least 1, and that no number comes with a
   * setting that is worked out.
   *
   * @throws NullPointerException if {@code mode} is {@code null}
   * @throws IllegalArgumentException if a fixed setting has no number or one below 1, or an
   *     automatic setting has a number
   */
  public PublishWorkersSetting {
    Objects.requireNonNull(mode, "Mode must not be null");
    if (mode == Mode.FIXED && (workers == null || workers < 1)) {
      throw new IllegalArgumentException("A fixed setting needs at least one worker");
    }
    if (mode == Mode.AUTO && workers != null) {
      throw new IllegalArgumentException("An automatic setting takes no number of workers");
    }
  }

  /**
   * The setting that leaves the number of workers to the server.
   *
   * @return the setting
   */
  public static PublishWorkersSetting auto() {
    return new PublishWorkersSetting(Mode.AUTO, null);
  }

  /**
   * A setting with a number of workers given by an administrator.
   *
   * @param workers the number of workers, at least 1
   * @return the setting
   */
  public static PublishWorkersSetting fixed(int workers) {
    return new PublishWorkersSetting(Mode.FIXED, workers);
  }
}
