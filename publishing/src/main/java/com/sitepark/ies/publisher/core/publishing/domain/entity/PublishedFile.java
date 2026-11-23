package com.sitepark.ies.publisher.core.publishing.domain.entity;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileType;
import org.jspecify.annotations.Nullable;

/**
 * A file a channel has published: where it lies, under which URL it is reached, and what it is.
 *
 * @param key the channel, entity and medium the file is published for; must not be {@code null}
 * @param type what the file is; must not be {@code null}
 * @param url the URL the file is reached under, relative to the channel; must not be {@code null}
 *     or blank
 * @param file the path of the file, relative to the channel's document root; must not be {@code
 *     null} or blank
 * @param md5 the MD5 checksum of the file's content, or {@code null} if none was recorded
 * @param mime the MIME type of the file, or {@code null} if none was recorded
 * @param uriProtection the protection the channel applies to the URL, or {@code null} if the URL
 *     is not protected
 */
public record PublishedFile(
    PublishedFileKey key,
    PublishedFileType type,
    String url,
    String file,
    @Nullable String md5,
    @Nullable String mime,
    @Nullable String uriProtection) {

  /**
   * Validates that key, type, URL and file are present.
   *
   * @throws IllegalArgumentException if {@code key} or {@code type} is {@code null}, or {@code url}
   *     or {@code file} is {@code null} or blank
   */
  public PublishedFile {
    if (key == null) {
      throw new IllegalArgumentException("Key must not be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("Type must not be null");
    }
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("URL must not be null or blank");
    }
    if (file == null || file.isBlank()) {
      throw new IllegalArgumentException("File must not be null or blank");
    }
  }

  /**
   * Creates a new builder for {@link PublishedFile}.
   *
   * @return a new, empty builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder pre-populated with the values of this file.
   *
   * @return a builder initialized from this instance
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  /** Builder for {@link PublishedFile} instances. */
  @SuppressWarnings("NullAway.Init")
  public static class Builder {
    private PublishedFileKey key;
    private PublishedFileType type;
    private String url;
    private String file;
    private @Nullable String md5;
    private @Nullable String mime;
    private @Nullable String uriProtection;

    /** Creates a new, empty builder. */
    public Builder() {}

    private Builder(PublishedFile file) {
      this.key = file.key;
      this.type = file.type;
      this.url = file.url;
      this.file = file.file;
      this.md5 = file.md5;
      this.mime = file.mime;
      this.uriProtection = file.uriProtection;
    }

    /**
     * Sets the channel, entity and medium the file is published for.
     *
     * @param key the key
     * @return this builder for chaining
     */
    public Builder key(PublishedFileKey key) {
      this.key = key;
      return this;
    }

    /**
     * Sets what the file is.
     *
     * @param type the type
     * @return this builder for chaining
     */
    public Builder type(PublishedFileType type) {
      this.type = type;
      return this;
    }

    /**
     * Sets the URL the file is reached under.
     *
     * @param url the URL, relative to the channel
     * @return this builder for chaining
     */
    public Builder url(String url) {
      this.url = url;
      return this;
    }

    /**
     * Sets the path of the file.
     *
     * @param file the path, relative to the channel's document root
     * @return this builder for chaining
     */
    public Builder file(String file) {
      this.file = file;
      return this;
    }

    /**
     * Sets the MD5 checksum of the file's content.
     *
     * @param md5 the checksum, or {@code null} if none was recorded
     * @return this builder for chaining
     */
    public Builder md5(@Nullable String md5) {
      this.md5 = md5;
      return this;
    }

    /**
     * Sets the MIME type of the file.
     *
     * @param mime the MIME type, or {@code null} if none was recorded
     * @return this builder for chaining
     */
    public Builder mime(@Nullable String mime) {
      this.mime = mime;
      return this;
    }

    /**
     * Sets the protection the channel applies to the URL.
     *
     * @param uriProtection the protection, or {@code null} if the URL is not protected
     * @return this builder for chaining
     */
    public Builder uriProtection(@Nullable String uriProtection) {
      this.uriProtection = uriProtection;
      return this;
    }

    /**
     * Builds a new {@link PublishedFile} instance from this builder.
     *
     * @return a new, validated file
     */
    public PublishedFile build() {
      return new PublishedFile(key, type, url, file, md5, mime, uriProtection);
    }
  }
}
