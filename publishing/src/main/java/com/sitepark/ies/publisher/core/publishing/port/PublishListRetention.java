package com.sitepark.ies.publisher.core.publishing.port;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRetentionSetting;
import java.time.Duration;

/**
 * How long lists that are over can still be looked at. Only what describes a list is kept, not its
 * tasks. The values may change while running; they apply the next time a list is over or the
 * lists are read.
 */
public interface PublishListRetention {

  /** The retention used when nothing else is configured: 20 lists, 24 hours. */
  PublishListRetention DEFAULT =
      new PublishListRetention() {
        @Override
        public int keptLists() {
          return PublishListRetentionSetting.DEFAULT.keptLists();
        }

        @Override
        public Duration lifetime() {
          return PublishListRetentionSetting.DEFAULT.lifetime();
        }
      };

  /**
   * The number of lists that are over to be kept; the youngest ones.
   *
   * @return the number, 0 to keep none
   */
  int keptLists();

  /**
   * How long a list that is over is kept, counted from the time it was over.
   *
   * @return the time, {@link Duration#ZERO} to keep none
   */
  Duration lifetime();
}
