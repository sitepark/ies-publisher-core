package com.sitepark.ies.publisher.core.channel.domain.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Describes a publishing channel together with its human-readable name and an optional description.
 *
 * @param id the unique identifier of the channel; must not be {@code null} or blank
 * @param name the display name of the channel; must not be {@code null} or blank
 * @param description an optional, free-form description of the channel, or {@code null} if none is
 *     provided
 */
@JsonDeserialize(builder = ChannelInfo.Builder.class)
public record ChannelInfo(String id, String name, @Nullable String description) {

  /**
   * Validates that the channel's identifier and name are present and not blank.
   *
   * @throws NullPointerException if {@code id} or {@code name} is {@code null}
   * @throws IllegalArgumentException if {@code id} or {@code name} is blank
   */
  public ChannelInfo {
    Objects.requireNonNull(id, "Channel ID must not be null or blank");
    if (id.isBlank()) {
      throw new IllegalArgumentException("Channel ID must not be null or blank");
    }
    Objects.requireNonNull(name, "Channel name must not be null or blank");
    if (name.isBlank()) {
      throw new IllegalArgumentException("Channel name must not be null or blank");
    }
  }

  /**
   * Creates a new builder for {@link ChannelInfo}.
   *
   * @return a new, empty builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for incrementally assembling a {@link ChannelInfo} instance. */
  @JsonPOJOBuilder(withPrefix = "")
  @SuppressWarnings("NullAway.Init")
  public static class Builder {
    private String id;
    private String name;
    private @Nullable String description;

    /** Creates an empty builder. */
    public Builder() {}

    /**
     * Sets the unique identifier of the channel.
     *
     * @param id the channel identifier
     * @return this builder for chaining
     */
    public Builder id(String id) {
      this.id = id;
      return this;
    }

    /**
     * Sets the display name of the channel.
     *
     * @param name the channel name
     * @return this builder for chaining
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the optional description of the channel.
     *
     * @param description the channel description, or {@code null} if none
     * @return this builder for chaining
     */
    public Builder description(@Nullable String description) {
      this.description = description;
      return this;
    }

    /**
     * Builds a new {@link ChannelInfo} instance from this builder.
     *
     * @return the assembled channel info
     */
    public ChannelInfo build() {
      return new ChannelInfo(id, name, description);
    }
  }
}
