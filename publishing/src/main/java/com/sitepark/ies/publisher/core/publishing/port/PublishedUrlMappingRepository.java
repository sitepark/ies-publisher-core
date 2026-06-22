package com.sitepark.ies.publisher.core.publishing.port;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedUrlMapping;
import java.util.List;

public interface PublishedUrlMappingRepository {

  List<PublishedUrlMapping> getPublishedUrlMapping(String channelD);

  List<PublishedUrlMapping> getPublishedUrlMappingByOwner(String channelId, String ownerId);

  String create(List<PublishedUrlMapping> mappings);

  void update(List<PublishedUrlMapping> mappings);

  int remove(List<String> ids);
}
