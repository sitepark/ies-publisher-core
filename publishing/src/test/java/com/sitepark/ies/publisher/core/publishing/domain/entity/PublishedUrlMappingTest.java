package com.sitepark.ies.publisher.core.publishing.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jparams.verifier.tostring.ToStringVerifier;
import com.sitepark.ies.sharedkernel.domain.UrlMappingMode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class PublishedUrlMappingTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(PublishedUrlMapping.class).suppress(Warning.NULL_FIELDS).verify();
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(PublishedUrlMapping.class).verify();
  }

  @Test
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  void buildWithNullSiteId_throwsException() {
    assertThatThrownBy(() -> base().siteId(null).build())
        .as("null siteId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithBlankSiteId_throwsException() {
    assertThatThrownBy(() -> base().siteId("  ").build())
        .as("blank siteId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  void buildWithNullChannelId_throwsException() {
    assertThatThrownBy(() -> base().channelId(null).build())
        .as("null channelId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithBlankChannelId_throwsException() {
    assertThatThrownBy(() -> base().channelId("  ").build())
        .as("blank channelId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  void buildWithNullPath_throwsException() {
    assertThatThrownBy(() -> base().path(null).build())
        .as("null path should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithBlankPath_throwsException() {
    assertThatThrownBy(() -> base().path("  ").build())
        .as("blank path should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  void buildWithNullOwnerId_throwsException() {
    assertThatThrownBy(() -> base().ownerId(null).build())
        .as("null ownerId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithBlankOwnerId_throwsException() {
    assertThatThrownBy(() -> base().ownerId("  ").build())
        .as("blank ownerId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  void buildWithNullMode_throwsException() {
    assertThatThrownBy(() -> base().mode(null).build())
        .as("null mode should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void hasSameContent_identicalContentDifferentId_returnsTrue() {
    PublishedUrlMapping a = base().id("1").build();
    PublishedUrlMapping b = base().id("2").build();

    assertThat(a.hasSameContent(b))
        .as("mappings with identical content but different id should be considered identical")
        .isTrue();
  }

  @Test
  void hasSameContent_differentPath_returnsFalse() {
    PublishedUrlMapping a = base().path("/a").build();
    PublishedUrlMapping b = base().path("/b").build();

    assertThat(a.hasSameContent(b))
        .as("mappings with different path should not be considered identical")
        .isFalse();
  }

  @Test
  void hasSameContent_differentMode_returnsFalse() {
    PublishedUrlMapping a = base().mode(UrlMappingMode.FORWARD).build();
    PublishedUrlMapping b = base().mode(UrlMappingMode.REDIRECT_PERMANENT).build();

    assertThat(a.hasSameContent(b))
        .as("mappings with different mode should not be considered identical")
        .isFalse();
  }

  @Test
  void hasSameContent_differentPriority_returnsFalse() {
    PublishedUrlMapping a = base().priority(0).build();
    PublishedUrlMapping b = base().priority(10).build();

    assertThat(a.hasSameContent(b))
        .as("mappings with different priority should not be considered identical")
        .isFalse();
  }

  @Test
  void toBuilderCopiesAllFieldsExceptChanged() {
    PublishedUrlMapping original = base().id("1").build();

    PublishedUrlMapping copy = original.toBuilder().id("2").build();

    assertThat(copy)
        .as("toBuilder should copy all fields and only differ in the changed id")
        .isEqualTo(base().id("2").build());
  }

  private static PublishedUrlMapping.Builder base() {
    return PublishedUrlMapping.builder()
        .siteId("site-1")
        .channelId("channel-1")
        .path("/path")
        .ownerId("owner-1")
        .mode(UrlMappingMode.FORWARD)
        .priority(0);
  }
}
