package com.sitepark.ies.publisher.core.channel.usecase;

import com.sitepark.ies.publisher.core.channel.domain.entity.ChannelInfo;
import com.sitepark.ies.publisher.core.channel.port.ChannelRepository;
import jakarta.inject.Inject;
import java.util.List;

/** Use case for retrieving information about all available publishing channels. */
public class GetAllChannelInfos {

  private final ChannelRepository channelRepository;

  /**
   * Creates the use case with the repository used to load channel information.
   *
   * @param channelRepository the repository providing access to channels
   */
  @Inject
  public GetAllChannelInfos(ChannelRepository channelRepository) {
    this.channelRepository = channelRepository;
  }

  /**
   * Retrieves information about all available channels.
   *
   * @return the list of all channels, or an empty list if none exist
   */
  public List<ChannelInfo> getAllChannelInfos() {
    return this.channelRepository.getAllChannelInfos();
  }
}
