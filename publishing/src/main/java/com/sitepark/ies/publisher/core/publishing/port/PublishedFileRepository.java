package com.sitepark.ies.publisher.core.publishing.port;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedFile;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileKey;
import java.util.Optional;

/**
 * Read access to the files a channel has published.
 *
 * <p>Only reading for now: the files are still written by the publisher outside this library.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface PublishedFileRepository {

  /**
   * Returns the file the key names, if the channel has published one.
   *
   * <p>A file that is about to be replaced is still returned while its successor is being written;
   * a file that is about to be deleted is not.
   *
   * @param key the channel, entity and medium to look for
   * @return the published file, or empty if the channel holds none for the key
   */
  Optional<PublishedFile> find(PublishedFileKey key);
}
