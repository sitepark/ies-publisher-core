module com.sitepark.ies.publisher.core.linkchecker {
  exports com.sitepark.ies.publisher.core.linkchecker.domain.entity;
  exports com.sitepark.ies.publisher.core.linkchecker.port;
  exports com.sitepark.ies.publisher.core.linkchecker.usecase;

  requires jakarta.inject;
  requires com.github.spotbugs.annotations;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires com.fasterxml.jackson.datatype.jsr310;

  opens com.sitepark.ies.publisher.core.linkchecker.domain.entity to
      com.fasterxml.jackson.databind;
}
