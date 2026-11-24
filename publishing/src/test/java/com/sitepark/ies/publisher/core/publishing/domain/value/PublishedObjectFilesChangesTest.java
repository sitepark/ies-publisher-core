package com.sitepark.ies.publisher.core.publishing.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedFileEntry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class PublishedObjectFilesChangesTest {

  private static final PublishedFilePath PATH =
      new PublishedFilePath(PublishedFileType.OBJECT, "/www/index.php");

  private static PublishedObjectFilesChanges changes(List<PublishedFilePath> claimed) {
    return new PublishedObjectFilesChanges(
        "55", "4711", List.of(), List.of(), List.of(), List.of(), claimed);
  }

  @Test
  void isEmptyWithoutAnything() {
    assertThat(changes(List.of()).isEmpty()).isTrue();
  }

  @Test
  void aClaimedPlaceIsSomethingToWrite() {
    assertThat(changes(List.of(PATH)).isEmpty()).isFalse();
  }

  @Test
  void anInsertIsSomethingToWrite() {
    PublishedFileEntry entry =
        PublishedFileEntry.builder()
            .key(PublishedFileKey.ofEntity("55", "4711"))
            .type(PublishedFileType.OBJECT)
            .url("/index.php")
            .file("/www/index.php")
            .build();
    PublishedObjectFilesChanges changes =
        new PublishedObjectFilesChanges(
            "55", "4711", List.of(entry), List.of(), List.of(), List.of(), List.of());

    assertThat(changes.isEmpty()).isFalse();
  }

  @Test
  void theListsAreCopies() {
    List<PublishedFilePath> claimed = new ArrayList<>(List.of(PATH));
    PublishedObjectFilesChanges changes = changes(claimed);
    claimed.clear();

    assertThat(changes.claimedPaths()).containsExactly(PATH);
  }

  @Test
  void rejectsMissingIdentifiers() {
    assertThatThrownBy(
            () ->
                new PublishedObjectFilesChanges(
                    "", "4711", List.of(), List.of(), List.of(), List.of(), List.of()))
        .as("channel")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new PublishedObjectFilesChanges(
                    "55", null, List.of(), List.of(), List.of(), List.of(), List.of()))
        .as("entity")
        .isInstanceOf(IllegalArgumentException.class);
  }
}
