module com.sitepark.ies.publisher.core.channel {
  exports com.sitepark.ies.publisher.core.channel.domain.entity;
  exports com.sitepark.ies.publisher.core.channel.port;
  exports com.sitepark.ies.publisher.core.channel.usecase;

  requires jakarta.inject;
  requires com.github.spotbugs.annotations;
  requires com.fasterxml.jackson.databind;
  requires com.fasterxml.jackson.datatype.jdk8;
  requires com.fasterxml.jackson.datatype.jsr310;
  requires com.sitepark.ies.sharedkernel;

  opens com.sitepark.ies.publisher.core.channel.domain.entity to
      com.fasterxml.jackson.databind;
}
