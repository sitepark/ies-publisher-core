package com.sitepark.ies.publisher.core.publishing.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jparams.verifier.tostring.ToStringVerifier;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class PublishedFileKeyTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(PublishedFileKey.class).suppress(Warning.NULL_FIELDS).verify();
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(PublishedFileKey.class).verify();
  }

  @Test
  void ofEntity_namesNoMedium() {
    assertThat(PublishedFileKey.ofEntity("55", "1118").isMedia())
        .as("the key of an entity's own file should name no medium")
        .isFalse();
  }

  @Test
  void ofMedia_namesTheMedium() {
    assertThat(PublishedFileKey.ofMedia("55", "1118", "35900").mediaId())
        .as("the key of a medium's file should name the medium")
        .isEqualTo("35900");
  }

  @Test
  void nullChannelId_throwsException() {
    assertThatThrownBy(() -> new PublishedFileKey(null, "1118", null))
        .as("null channelId should throw a NullPointerException")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void blankChannelId_throwsException() {
    assertThatThrownBy(() -> new PublishedFileKey(" ", "1118", null))
        .as("blank channelId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void nullEntityId_throwsException() {
    assertThatThrownBy(() -> new PublishedFileKey("55", null, null))
        .as("null entityId should throw a NullPointerException")
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void blankEntityId_throwsException() {
    assertThatThrownBy(() -> new PublishedFileKey("55", " ", null))
        .as("blank entityId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void blankMediaId_throwsException() {
    assertThatThrownBy(() -> new PublishedFileKey("55", "1118", " "))
        .as("blank mediaId should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }
}
