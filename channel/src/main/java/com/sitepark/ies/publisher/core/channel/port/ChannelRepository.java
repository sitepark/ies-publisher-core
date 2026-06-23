package com.sitepark.ies.publisher.core.channel.port;

import com.sitepark.ies.publisher.core.channel.domain.entity.ChannelInfo;
import java.util.List;

/**
 * Provides read access to the channels known to the publishing system.
 *
 * <p>Implementations live outside this library in adapter modules.
 */
public interface ChannelRepository {

  /**
   * Returns information about all available channels.
   *
   * @return the list of all channels, or an empty list if none exist
   */
  List<ChannelInfo> getAllChannelInfos();
}
