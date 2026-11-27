package com.sitepark.ies.publisher.core.publishing.domain.entity;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFilePath;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileStatus;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileType;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Everything a channel stores about a file it published: what {@link PublishedFile} answers, and
 * what the publisher needs to write, replace and delete it.
 *
 * @param key the channel, entity and medium the file is published for; must not be {@code null}
 * @param type what the file is; must not be {@code null}
 * @param status where the file stands; must not be {@code null}
 * @param url the URL the file is reached under, relative to the channel; must not be {@code null}
 * @param file the path of the file, relative to the channel's document root; must not be {@code
 *     null} or blank
 * @param mainMedium whether the file is the main medium of its entity
 * @param mediaMetaFile the file describing the medium, or {@code null}
 * @param templateId the template the file was generated with, or {@code null}
 * @param moduleId the module of the template, or {@code null}
 * @param moduleUri the URI of the template within its module, or {@code null}
 * @param collision the entity whose file took the place first, for a file moved aside because of
 *     it, or {@code null}
 * @param size the size of the file in bytes
 * @param timestamp the time the file was written, in milliseconds since the epoch
 * @param md5 the MD5 checksum of the file's content, or {@code null}
 * @param mime the MIME type of the file, or {@code null}
 * @param uriProtection the protection the channel applies to the URL, or {@code null}
 * @param webIndexEnabled whether the file is indexed for the site search
 * @param webIndexDefaultFields the fields the site search indexes by default, as JSON, or {@code
 *     null}
 */
public record PublishedFileEntry(
    PublishedFileKey key,
    PublishedFileType type,
    PublishedFileStatus status,
    String url,
    String file,
    boolean mainMedium,
    @Nullable String mediaMetaFile,
    @Nullable String templateId,
    @Nullable String moduleId,
    @Nullable String moduleUri,
    @Nullable String collision,
    long size,
    long timestamp,
    @Nullable String md5,
    @Nullable String mime,
    @Nullable String uriProtection,
    boolean webIndexEnabled,
    @Nullable String webIndexDefaultFields) {

  /**
   * Validates that key, type, status, URL and file are present.
   *
   * @throws NullPointerException if {@code key}, {@code type}, {@code status}, {@code url} or {@code
   *     file} is {@code null}
   * @throws IllegalArgumentException if {@code file} is blank
   */
  public PublishedFileEntry {
    Objects.requireNonNull(key, "Key must not be null");
    Objects.requireNonNull(type, "Type must not be null");
    Objects.requireNonNull(status, "Status must not be null");
    Objects.requireNonNull(url, "URL must not be null");
    Objects.requireNonNull(file, "File must not be null or blank");
    if (file.isBlank()) {
      throw new IllegalArgumentException("File must not be null or blank");
    }
  }

  /**
   * Returns the place the file takes in its channel.
   *
   * @return type and path of the file
   */
  public PublishedFilePath path() {
    return new PublishedFilePath(this.type, this.file);
  }

  /**
   * Returns the same file in another status.
   *
   * @param status the status
   * @return the file with the status
   */
  public PublishedFileEntry withStatus(PublishedFileStatus status) {
    return this.toBuilder().status(status).build();
  }

  /**
   * Returns what a link to the file resolves to.
   *
   * @return the published file
   */
  public PublishedFile toPublishedFile() {
    return new PublishedFile(
        this.key, this.type, this.url, this.file, this.md5, this.mime, this.uriProtection);
  }

  /**
   * Creates a new builder for {@link PublishedFileEntry}. A new entry is published, enabled for the
   * site search and otherwise empty.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder pre-populated with the values of this entry.
   *
   * @return a builder initialized from this instance
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  /** Builder for {@link PublishedFileEntry} instances. */
  @SuppressWarnings({"NullAway.Init", "PMD.TooManyFields", "PMD.TooManyMethods"})
  public static class Builder {
    private PublishedFileKey key;
    private PublishedFileType type;
    private PublishedFileStatus status = PublishedFileStatus.PUBLISHED;
    private String url;
    private String file;
    private boolean mainMedium;
    private @Nullable String mediaMetaFile;
    private @Nullable String templateId;
    private @Nullable String moduleId;
    private @Nullable String moduleUri;
    private @Nullable String collision;
    private long size;
    private long timestamp;
    private @Nullable String md5;
    private @Nullable String mime;
    private @Nullable String uriProtection;
    private boolean webIndexEnabled = true;
    private @Nullable String webIndexDefaultFields;

    /** Creates a new, empty builder. */
    public Builder() {}

    private Builder(PublishedFileEntry entry) {
      this.key = entry.key;
      this.type = entry.type;
      this.status = entry.status;
      this.url = entry.url;
      this.file = entry.file;
      this.mainMedium = entry.mainMedium;
      this.mediaMetaFile = entry.mediaMetaFile;
      this.templateId = entry.templateId;
      this.moduleId = entry.moduleId;
      this.moduleUri = entry.moduleUri;
      this.collision = entry.collision;
      this.size = entry.size;
      this.timestamp = entry.timestamp;
      this.md5 = entry.md5;
      this.mime = entry.mime;
      this.uriProtection = entry.uriProtection;
      this.webIndexEnabled = entry.webIndexEnabled;
      this.webIndexDefaultFields = entry.webIndexDefaultFields;
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
     * Sets where the file stands.
     *
     * @param status the status
     * @return this builder for chaining
     */
    public Builder status(PublishedFileStatus status) {
      this.status = status;
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
     * Sets whether the file is the main medium of its entity.
     *
     * @param mainMedium whether it is
     * @return this builder for chaining
     */
    public Builder mainMedium(boolean mainMedium) {
      this.mainMedium = mainMedium;
      return this;
    }

    /**
     * Sets the file describing the medium.
     *
     * @param mediaMetaFile the file, or {@code null}
     * @return this builder for chaining
     */
    public Builder mediaMetaFile(@Nullable String mediaMetaFile) {
      this.mediaMetaFile = mediaMetaFile;
      return this;
    }

    /**
     * Sets the template the file was generated with.
     *
     * @param templateId the template, or {@code null}
     * @param moduleId its module, or {@code null}
     * @param moduleUri its URI within the module, or {@code null}
     * @return this builder for chaining
     */
    public Builder template(
        @Nullable String templateId, @Nullable String moduleId, @Nullable String moduleUri) {
      this.templateId = templateId;
      this.moduleId = moduleId;
      this.moduleUri = moduleUri;
      return this;
    }

    /**
     * Sets the entity whose file took the place first.
     *
     * @param collision the entity, or {@code null}
     * @return this builder for chaining
     */
    public Builder collision(@Nullable String collision) {
      this.collision = collision;
      return this;
    }

    /**
     * Sets the size of the file.
     *
     * @param size the size in bytes
     * @return this builder for chaining
     */
    public Builder size(long size) {
      this.size = size;
      return this;
    }

    /**
     * Sets the time the file was written.
     *
     * @param timestamp milliseconds since the epoch
     * @return this builder for chaining
     */
    public Builder timestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    /**
     * Sets the MD5 checksum of the file's content.
     *
     * @param md5 the checksum, or {@code null}
     * @return this builder for chaining
     */
    public Builder md5(@Nullable String md5) {
      this.md5 = md5;
      return this;
    }

    /**
     * Sets the MIME type of the file.
     *
     * @param mime the MIME type, or {@code null}
     * @return this builder for chaining
     */
    public Builder mime(@Nullable String mime) {
      this.mime = mime;
      return this;
    }

    /**
     * Sets the protection the channel applies to the URL.
     *
     * @param uriProtection the protection, or {@code null}
     * @return this builder for chaining
     */
    public Builder uriProtection(@Nullable String uriProtection) {
      this.uriProtection = uriProtection;
      return this;
    }

    /**
     * Sets how the site search indexes the file.
     *
     * @param enabled whether the file is indexed
     * @param defaultFields the fields indexed by default, as JSON, or {@code null}
     * @return this builder for chaining
     */
    public Builder webIndex(boolean enabled, @Nullable String defaultFields) {
      this.webIndexEnabled = enabled;
      this.webIndexDefaultFields = defaultFields;
      return this;
    }

    /**
     * Builds a new {@link PublishedFileEntry} instance from this builder.
     *
     * @return a new, validated entry
     */
    public PublishedFileEntry build() {
      return new PublishedFileEntry(
          key,
          type,
          status,
          url,
          file,
          mainMedium,
          mediaMetaFile,
          templateId,
          moduleId,
          moduleUri,
          collision,
          size,
          timestamp,
          md5,
          mime,
          uriProtection,
          webIndexEnabled,
          webIndexDefaultFields);
    }
  }
}
