package com.sitepark.ies.publisher.core.channel.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

public record ChannelInfo(String id, String name, String description) {

  public ChannelInfo {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Channel ID must not be null or blank");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Channel name must not be null or blank");
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static class Builder {
    private String id;
    private String name;
    private String description;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public ChannelInfo build() {
      return new ChannelInfo(id, name, description);
    }
  }
}
