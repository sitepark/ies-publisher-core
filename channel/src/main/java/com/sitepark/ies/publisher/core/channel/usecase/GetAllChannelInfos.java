package com.sitepark.ies.publisher.core.channel.usecase;

import com.sitepark.ies.publisher.core.channel.domain.entity.ChannelInfo;
import com.sitepark.ies.publisher.core.channel.port.ChannelRepository;
import jakarta.inject.Inject;
import java.util.List;

public class GetAllChannelInfos {

  private final ChannelRepository channelRepository;

  @Inject
  public GetAllChannelInfos(ChannelRepository channelRepository) {
    this.channelRepository = channelRepository;
  }

  public List<ChannelInfo> getAllChannelInfos() {
    return this.channelRepository.getAllChannelInfos();
  }
}
