package com.sitepark.ies.publisher.core.publishing.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFilePath;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileStatus;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileType;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class PublishedFileEntryTest {

  private static final PublishedFileKey KEY = PublishedFileKey.ofMedia("55", "4711", "815");

  private static PublishedFileEntry.Builder complete() {
    return PublishedFileEntry.builder()
        .key(KEY)
        .type(PublishedFileType.MEDIA)
        .status(PublishedFileStatus.STALE)
        .url("/media/a.png")
        .file("/www/media/a.png")
        .mainMedium(true)
        .mediaMetaFile("/www/media/a.php")
        .template("12", "sitekit", "/media.spml")
        .collision("4712")
        .size(42)
        .timestamp(1000)
        .md5("abc")
        .mime("image/png")
        .uriProtection("protected")
        .webIndex(false, "{\"title\":\"A\"}");
  }

  @Test
  void equalsContract() {
    EqualsVerifier.forClass(PublishedFileEntry.class).suppress(Warning.NULL_FIELDS).verify();
  }

  @Test
  void aNewEntryIsPublishedAndIndexed() {
    PublishedFileEntry entry =
        PublishedFileEntry.builder()
            .key(KEY)
            .type(PublishedFileType.MEDIA)
            .url("/media/a.png")
            .file("/www/media/a.png")
            .build();

    assertThat(entry.status()).as("status").isEqualTo(PublishedFileStatus.PUBLISHED);
    assertThat(entry.webIndexEnabled()).as("web index").isTrue();
  }

  @Test
  void toBuilderKeepsEverything() {
    PublishedFileEntry entry = complete().build();

    assertThat(entry.toBuilder().build()).as("copy").isEqualTo(entry);
  }

  @Test
  void withStatusChangesOnlyTheStatus() {
    PublishedFileEntry entry = complete().build();

    PublishedFileEntry trashed = entry.withStatus(PublishedFileStatus.TRASHED);

    assertThat(trashed.status()).as("status").isEqualTo(PublishedFileStatus.TRASHED);
    assertThat(trashed.withStatus(PublishedFileStatus.STALE)).as("rest").isEqualTo(entry);
  }

  @Test
  void pathIsTypeAndFile() {
    assertThat(complete().build().path())
        .isEqualTo(new PublishedFilePath(PublishedFileType.MEDIA, "/www/media/a.png"));
  }

  @Test
  void toPublishedFileAnswersALink() {
    PublishedFile file = complete().build().toPublishedFile();

    assertThat(file)
        .isEqualTo(
            new PublishedFile(
                KEY,
                PublishedFileType.MEDIA,
                "/media/a.png",
                "/www/media/a.png",
                "abc",
                "image/png",
                "protected"));
  }

  @Test
  void rejectsMissingValues() {
    assertThatThrownBy(() -> complete().key(null).build())
        .as("key")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> complete().type(null).build())
        .as("type")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> complete().status(null).build())
        .as("status")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> complete().url(null).build())
        .as("url")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> complete().file(" ").build())
        .as("file")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> complete().file(null).build())
        .as("no file")
        .isInstanceOf(IllegalArgumentException.class);
  }
}
