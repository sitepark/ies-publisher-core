package com.sitepark.ies.publisher.core.publishing.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jparams.verifier.tostring.ToStringVerifier;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileType;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class PublishedFileTest {

  @Test
  void testEquals() {
    EqualsVerifier.forClass(PublishedFile.class).suppress(Warning.NULL_FIELDS).verify();
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(PublishedFile.class).verify();
  }

  @Test
  void toBuilder_keepsEveryValue() {
    PublishedFile file = base().md5("abc").mime("text/html").uriProtection("+12").build();

    assertThat(file.toBuilder().build())
        .as("a file rebuilt from its builder should equal the original")
        .isEqualTo(file);
  }

  @Test
  void optionalValuesMayBeAbsent() {
    PublishedFile file = base().build();

    assertThat(file.uriProtection()).as("a file without protection should report none").isNull();
  }

  @Test
  void buildWithNullKey_throwsException() {
    assertThatThrownBy(() -> base().key(null).build())
        .as("null key should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithNullType_throwsException() {
    assertThatThrownBy(() -> base().type(null).build())
        .as("null type should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithNullUrl_throwsException() {
    assertThatThrownBy(() -> base().url(null).build())
        .as("null url should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithBlankUrl_throwsException() {
    assertThatThrownBy(() -> base().url(" ").build())
        .as("blank url should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithNullFile_throwsException() {
    assertThatThrownBy(() -> base().file(null).build())
        .as("null file should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void buildWithBlankFile_throwsException() {
    assertThatThrownBy(() -> base().file(" ").build())
        .as("blank file should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static PublishedFile.Builder base() {
    return PublishedFile.builder()
        .key(PublishedFileKey.ofEntity("55", "1118"))
        .type(PublishedFileType.OBJECT)
        .url("/index.php")
        .file("/index.php");
  }
}
