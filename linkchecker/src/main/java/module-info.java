import org.jspecify.annotations.NullMarked;

/** Core module for the IES link checker: validates external links and manages their scheduling. */
@NullMarked
module com.sitepark.ies.publisher.core.linkchecker {
  exports com.sitepark.ies.publisher.core.linkchecker.domain.entity;
  exports com.sitepark.ies.publisher.core.linkchecker.domain.exception;
  exports com.sitepark.ies.publisher.core.linkchecker.port;
  exports com.sitepark.ies.publisher.core.linkchecker.usecase;

  requires jakarta.inject;
  requires com.fasterxml.jackson.annotation;
  requires static org.jspecify;
  requires com.sitepark.ies.sharedkernel;

  opens com.sitepark.ies.publisher.core.linkchecker.domain.entity;
}
