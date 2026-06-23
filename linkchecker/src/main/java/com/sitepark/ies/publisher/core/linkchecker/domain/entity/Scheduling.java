package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Strategy describing when the link checker is run automatically. */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(name = "daily", value = DailyScheduling.class),
})
public interface Scheduling {
  /**
   * Returns the discriminator type identifying the concrete scheduling strategy.
   *
   * @return the scheduling type identifier
   */
  String getType();
}
