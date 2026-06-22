package com.sitepark.ies.publisher.core.publishing.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedUrlMapping;
import com.sitepark.ies.sharedkernel.domain.UrlMappingMode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class OwnerUrlMappingsTest {

  @Test
  void testEquals() {
    OwnerUrlMappings a1 =
        OwnerUrlMappings.builder().ownerId("o1").channelId("c1").mappings(List.of()).build();
    OwnerUrlMappings a2 =
        OwnerUrlMappings.builder().ownerId("o1").channelId("c1").mappings(List.of()).build();
    OwnerUrlMappings differentOwner =
        OwnerUrlMappings.builder().ownerId("o2").channelId("c1").mappings(List.of()).build();
    OwnerUrlMappings differentChannel =
        OwnerUrlMappings.builder().ownerId("o1").channelId("c2").mappings(List.of()).build();

    assertThat(a1).as("equal fields should result in equality").isEqualTo(a2);
    assertThat(a1.hashCode())
        .as("equal instances should have the same hashCode")
        .isEqualTo(a2.hashCode());
    assertThat(a1).as("different ownerId should result in inequality").isNotEqualTo(differentOwner);
    assertThat(a1)
        .as("different channelId should result in inequality")
        .isNotEqualTo(differentChannel);
  }

  @Test
  void testToString() {
    OwnerUrlMappings ownerUrlMappings =
        OwnerUrlMappings.builder()
            .ownerId("owner-1")
            .channelId("channel-1")
            .mappings(List.of())
            .build();

    assertThat(ownerUrlMappings.toString())
        .as("toString should contain ownerId and channelId")
        .contains("owner-1", "channel-1");
  }

  @Test
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  void buildWithNullOwnerId_throwsException() {
    assertThatThrownBy(
            () ->
                OwnerUrlMappings.builder()
                    .ownerId(null)
                    .channelId("channel-1")
                    .mappings(List.of())
                    .build())
        .as("null ownerId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithBlankOwnerId_throwsException() {
    assertThatThrownBy(
            () ->
                OwnerUrlMappings.builder()
                    .ownerId("  ")
                    .channelId("channel-1")
                    .mappings(List.of())
                    .build())
        .as("blank ownerId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  void buildWithNullChannelId_throwsException() {
    assertThatThrownBy(
            () ->
                OwnerUrlMappings.builder()
                    .ownerId("owner-1")
                    .channelId(null)
                    .mappings(List.of())
                    .build())
        .as("null channelId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithBlankChannelId_throwsException() {
    assertThatThrownBy(
            () ->
                OwnerUrlMappings.builder()
                    .ownerId("owner-1")
                    .channelId("  ")
                    .mappings(List.of())
                    .build())
        .as("blank channelId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  void buildWithNullMappings_throwsException() {
    assertThatThrownBy(
            () ->
                OwnerUrlMappings.builder()
                    .ownerId("owner-1")
                    .channelId("channel-1")
                    .mappings(null)
                    .build())
        .as("null mappings list should throw a NullPointerException")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void buildWithMappingFromDifferentOwner_throwsException() {
    PublishedUrlMapping wrongOwner =
        PublishedUrlMapping.builder()
            .siteId("site-1")
            .channelId("channel-1")
            .path("/path")
            .ownerId("other-owner")
            .mode(UrlMappingMode.FORWARD)
            .build();

    assertThatThrownBy(
            () ->
                OwnerUrlMappings.builder()
                    .ownerId("owner-1")
                    .channelId("channel-1")
                    .mappings(List.of(wrongOwner))
                    .build())
        .as("mapping with a different ownerId should violate the aggregate invariant")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithMappingFromDifferentChannel_throwsException() {
    PublishedUrlMapping wrongChannel =
        PublishedUrlMapping.builder()
            .siteId("site-1")
            .channelId("other-channel")
            .path("/path")
            .ownerId("owner-1")
            .mode(UrlMappingMode.FORWARD)
            .build();

    assertThatThrownBy(
            () ->
                OwnerUrlMappings.builder()
                    .ownerId("owner-1")
                    .channelId("channel-1")
                    .mappings(List.of(wrongChannel))
                    .build())
        .as("mapping with a different channelId should violate the aggregate invariant")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void isEmpty_withNoMappings_returnsTrue() {
    OwnerUrlMappings ownerUrlMappings =
        OwnerUrlMappings.builder()
            .ownerId("owner-1")
            .channelId("channel-1")
            .mappings(List.of())
            .build();

    assertThat(ownerUrlMappings.isEmpty())
        .as("OwnerUrlMappings without entries should return isEmpty() = true")
        .isTrue();
  }

  @Test
  void isEmpty_withMappings_returnsFalse() {
    PublishedUrlMapping mapping =
        PublishedUrlMapping.builder()
            .siteId("site-1")
            .channelId("channel-1")
            .path("/path")
            .ownerId("owner-1")
            .mode(UrlMappingMode.FORWARD)
            .build();
    OwnerUrlMappings ownerUrlMappings =
        OwnerUrlMappings.builder()
            .ownerId("owner-1")
            .channelId("channel-1")
            .mappings(List.of(mapping))
            .build();

    assertThat(ownerUrlMappings.isEmpty())
        .as("OwnerUrlMappings with entries should return isEmpty() = false")
        .isFalse();
  }
}
