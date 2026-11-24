package com.sitepark.ies.publisher.core.publishing.domain.value;

/**
 * Where a published file stands between being written and being deleted.
 *
 * <p>The status is part of what identifies a stored file: the same file can be stored once as
 * published and once as trashed, until the trashed one is deleted.
 */
public enum PublishedFileStatus {

  /** The file is published and matches what lies in the channel. */
  PUBLISHED,

  /**
   * The file is being generated again and has not been confirmed yet. Left behind by a generation
   * that was cut short; a published file of the same entity wins over it.
   */
  STALE,

  /**
   * The file is no longer published: its entity was revoked, or it was generated to another place.
   * The file in the channel is deleted once nothing depends on it any more.
   */
  TRASHED
}
