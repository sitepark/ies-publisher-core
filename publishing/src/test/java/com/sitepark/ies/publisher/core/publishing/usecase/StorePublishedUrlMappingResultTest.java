package com.sitepark.ies.publisher.core.publishing.usecase;

import com.jparams.verifier.tostring.ToStringVerifier;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class StorePublishedUrlMappingResultTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(StorePublishedUrlMappingResult.class)
        .suppress(Warning.NULL_FIELDS)
        .verify();
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(StorePublishedUrlMappingResult.class).verify();
  }
}
