import org.jspecify.annotations.NullMarked;

/**
 * Core module for the automatic translation of publications: statistics about translated and
 * translatable texts.
 */
@NullMarked
module com.sitepark.ies.publisher.core.translation {
  exports com.sitepark.ies.publisher.core.translation.domain.entity;
  exports com.sitepark.ies.publisher.core.translation.domain.exception;
  exports com.sitepark.ies.publisher.core.translation.port;
  exports com.sitepark.ies.publisher.core.translation.usecase;

  requires jakarta.inject;
  requires static org.jspecify;
  requires com.sitepark.ies.sharedkernel;
}
