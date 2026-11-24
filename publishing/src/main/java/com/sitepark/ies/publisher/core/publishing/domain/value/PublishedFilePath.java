package com.sitepark.ies.publisher.core.publishing.domain.value;

/**
 * The place a file takes in a channel. Two files of the same type cannot take the same place: the
 * one published last claims it.
 *
 * @param type what the file is; must not be {@code null}
 * @param file the path of the file, relative to the channel's document root; must not be {@code
 *     null} or blank
 */
public record PublishedFilePath(PublishedFileType type, String file) {

  /**
   * Validates that type and path are present.
   *
   * @throws IllegalArgumentException if {@code type} is {@code null} or {@code file} is {@code
   *     null} or blank
   */
  public PublishedFilePath {
    if (type == null) {
      throw new IllegalArgumentException("Type must not be null");
    }
    if (file == null || file.isBlank()) {
      throw new IllegalArgumentException("File must not be null or blank");
    }
  }
}
