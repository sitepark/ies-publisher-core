package com.sitepark.ies.publisher.core.channel.port;

import com.sitepark.ies.publisher.core.channel.domain.entity.ChannelInfo;
import java.util.List;

public interface ChannelRepository {
  List<ChannelInfo> getAllChannelInfos();
}
