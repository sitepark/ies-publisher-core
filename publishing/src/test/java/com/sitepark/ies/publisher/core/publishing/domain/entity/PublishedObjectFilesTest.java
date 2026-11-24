package com.sitepark.ies.publisher.core.publishing.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFilePath;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileStatus;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileType;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedObjectFilesChanges;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Publishing an entity again has to end where the legacy publisher ended: its files at their places,
 * everything else of it trashed - and only the difference written.
 */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class PublishedObjectFilesTest {

  private static final String CHANNEL = "55";

  private static final String ENTITY = "4711";

  private static final PublishedFileKey PAGE_KEY = PublishedFileKey.ofEntity(CHANNEL, ENTITY);

  private static final PublishedFileKey IMAGE_KEY =
      PublishedFileKey.ofMedia(CHANNEL, ENTITY, "815");

  private static PublishedFileEntry page(String file) {
    return PublishedFileEntry.builder()
        .key(PAGE_KEY)
        .type(PublishedFileType.OBJECT)
        .url("/" + file)
        .file("/www/" + file)
        .build();
  }

  private static PublishedFileEntry image(String file) {
    return PublishedFileEntry.builder()
        .key(IMAGE_KEY)
        .type(PublishedFileType.MEDIA)
        .url("/media/" + file)
        .file("/www/media/" + file)
        .mainMedium(true)
        .build();
  }

  private static PublishedObjectFiles stored(PublishedFileEntry... files) {
    return new PublishedObjectFiles(CHANNEL, ENTITY, List.of(files));
  }

  private static PublishedObjectFilesChanges publish(
      PublishedObjectFiles stored, PublishedFileEntry... files) {
    return stored.changesTo(stored.published(List.of(files)));
  }

  @Test
  void publishingTheSameFilesAgainWritesNothing() {
    PublishedObjectFiles stored = stored(page("index.php"), image("a.png"));

    PublishedObjectFilesChanges changes = publish(stored, image("a.png"), page("index.php"));

    assertThat(changes.isEmpty()).as("nothing changed, nothing to write").isTrue();
  }

  @Test
  void aChangedChecksumIsUpdatedInPlace() {
    PublishedObjectFiles stored = stored(page("index.php"));
    PublishedFileEntry regenerated = page("index.php").toBuilder().md5("new").size(42).build();

    PublishedObjectFilesChanges changes = publish(stored, regenerated);

    assertThat(changes.updates()).as("updates").containsExactly(regenerated);
    assertThat(changes.inserts()).as("inserts").isEmpty();
    assertThat(changes.deletes()).as("deletes").isEmpty();
    assertThat(changes.claimedPaths()).as("the place was held already").isEmpty();
  }

  @Test
  void theFirstPublicationClaimsItsPlace() {
    PublishedObjectFiles stored = stored();

    PublishedObjectFilesChanges changes = publish(stored, page("index.php"));

    assertThat(changes.inserts()).as("inserts").containsExactly(page("index.php"));
    assertThat(changes.claimedPaths())
        .as("files of other entities at the place are deleted")
        .containsExactly(new PublishedFilePath(PublishedFileType.OBJECT, "/www/index.php"));
  }

  @Test
  void aMovedFileTakesItsNewPlaceAndTrashesTheOldOne() {
    PublishedObjectFiles stored = stored(page("old.php"));

    PublishedObjectFiles next = stored.published(List.of(page("new.php")));
    PublishedObjectFilesChanges changes = stored.changesTo(next);

    assertThat(changes.inserts()).as("inserts").containsExactly(page("new.php"));
    assertThat(changes.trashes()).as("trashes").containsExactly(page("old.php"));
    assertThat(changes.claimedPaths())
        .as("claimed")
        .containsExactly(new PublishedFilePath(PublishedFileType.OBJECT, "/www/new.php"));
    assertThat(next.trashed())
        .as("the old file is to be deleted from the channel")
        .containsExactly(page("old.php").withStatus(PublishedFileStatus.TRASHED));
    assertThat(next.find(PAGE_KEY))
        .as("a link resolves to the new place")
        .contains(page("new.php"));
  }

  @Test
  void aMediumNoLongerPublishedIsTrashed() {
    PublishedObjectFiles stored = stored(page("index.php"), image("a.png"));

    PublishedObjectFilesChanges changes = publish(stored, page("index.php"));

    assertThat(changes.trashes()).as("trashes").containsExactly(image("a.png"));
    assertThat(changes.inserts()).as("inserts").isEmpty();
  }

  @Test
  void aStaleLeftoverAtThePlaceIsReplaced() {
    PublishedFileEntry stale = page("index.php").withStatus(PublishedFileStatus.STALE);
    PublishedObjectFiles stored = stored(stale);

    PublishedObjectFilesChanges changes = publish(stored, page("index.php"));

    assertThat(changes.deletes()).as("deletes").containsExactly(stale);
    assertThat(changes.inserts()).as("inserts").containsExactly(page("index.php"));
  }

  @Test
  void aStaleLeftoverElsewhereIsTrashed() {
    PublishedFileEntry stale = image("a.png").withStatus(PublishedFileStatus.STALE);
    PublishedObjectFiles stored = stored(page("index.php"), stale);

    PublishedObjectFilesChanges changes = publish(stored, page("index.php"));

    assertThat(changes.trashes()).as("trashes").containsExactly(stale);
  }

  @Test
  void aTrashedFileAtTheRepublishedPlaceIsDeleted() {
    PublishedFileEntry trashed = page("index.php").withStatus(PublishedFileStatus.TRASHED);
    PublishedObjectFiles stored = stored(trashed);

    PublishedObjectFiles next = stored.published(List.of(page("index.php")));
    PublishedObjectFilesChanges changes = stored.changesTo(next);

    assertThat(changes.deletes())
        .as("the file must not be deleted from the channel after all")
        .containsExactly(trashed);
    assertThat(changes.inserts()).as("inserts").containsExactly(page("index.php"));
    assertThat(next.trashed()).as("nothing left to delete").isEmpty();
  }

  @Test
  void aFileTrashedBeforeStaysTrashed() {
    PublishedFileEntry trashed = image("a.png").withStatus(PublishedFileStatus.TRASHED);
    PublishedObjectFiles stored = stored(page("index.php"), trashed);

    PublishedObjectFiles next = stored.published(List.of(page("index.php")));

    assertThat(stored.changesTo(next).isEmpty()).as("nothing to write").isTrue();
    assertThat(next.trashed()).as("still to be deleted").containsExactly(trashed);
  }

  @Test
  void aFileBothPublishedAndTrashedKeepsTheTrashedOne() {
    PublishedFileEntry published = image("a.png");
    PublishedFileEntry trashed = image("a.png").withStatus(PublishedFileStatus.TRASHED);
    PublishedObjectFiles stored = stored(page("index.php"), published, trashed);

    PublishedObjectFilesChanges changes = publish(stored, page("index.php"));

    assertThat(changes.deletes())
        .as("the published one goes, the trashed twin is there already")
        .containsExactly(published);
    assertThat(changes.trashes()).as("trashes").isEmpty();
  }

  @Test
  void anotherKeyAtTheClaimedPlaceIsDeleted() {
    PublishedFileEntry imageThere =
        image("a.png").toBuilder().type(PublishedFileType.OBJECT).file("/www/index.php").build();
    PublishedObjectFiles stored = stored(imageThere);

    PublishedObjectFilesChanges changes = publish(stored, page("index.php"));

    assertThat(changes.deletes()).as("deletes").containsExactly(imageThere);
    assertThat(changes.inserts()).as("inserts").containsExactly(page("index.php"));
  }

  @Test
  void aKeyPublishedTwiceEndsAtItsLastPlace() {
    PublishedObjectFiles stored = stored();

    PublishedObjectFiles next = stored.published(List.of(page("first.php"), page("second.php")));

    assertThat(next.find(PAGE_KEY)).as("published").contains(page("second.php"));
    assertThat(next.trashed())
        .as("the first place is to be deleted again")
        .containsExactly(page("first.php").withStatus(PublishedFileStatus.TRASHED));
  }

  @Test
  void aPublishedFileWinsOverAStaleOne() {
    PublishedFileEntry stale = page("index.php").withStatus(PublishedFileStatus.STALE);
    PublishedObjectFiles stored = stored(stale, page("index.php"));

    assertThat(stored.find(PAGE_KEY)).as("found").contains(page("index.php"));
  }

  @Test
  void aStaleFileIsFoundWhileNothingElseIs() {
    PublishedFileEntry stale = page("index.php").withStatus(PublishedFileStatus.STALE);

    assertThat(stored(stale).find(PAGE_KEY)).as("found").contains(stale);
  }

  @Test
  void aTrashedFileIsNeverFound() {
    PublishedFileEntry trashed = page("index.php").withStatus(PublishedFileStatus.TRASHED);

    assertThat(stored(trashed).find(PAGE_KEY)).as("found").isEmpty();
  }

  @Test
  void aFileIsNewOrMovedUnlessItsPlaceAndUrlAreStored() {
    PublishedObjectFiles stored = stored(page("index.php").withStatus(PublishedFileStatus.TRASHED));

    assertThat(stored.isNewOrMoved(page("index.php")))
        .as("same place and url, even trashed")
        .isFalse();
    assertThat(stored.isNewOrMoved(page("other.php"))).as("other place").isTrue();
    assertThat(stored.isNewOrMoved(page("index.php").toBuilder().url("/moved").build()))
        .as("other url")
        .isTrue();
  }

  @Test
  void rejectsAFileOfAnotherEntity() {
    PublishedFileEntry foreign =
        page("index.php").toBuilder().key(PublishedFileKey.ofEntity(CHANNEL, "1")).build();

    assertThatThrownBy(() -> stored(foreign))
        .as("stored")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> stored().published(List.of(foreign)))
        .as("published")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsChangesToAnotherEntity() {
    PublishedObjectFiles other = new PublishedObjectFiles(CHANNEL, "1", List.of());

    assertThatThrownBy(() -> stored().changesTo(other))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMissingIdentifiers() {
    assertThatThrownBy(() -> new PublishedObjectFiles(" ", ENTITY, List.of()))
        .as("channel")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PublishedObjectFiles(CHANNEL, null, List.of()))
        .as("entity")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aPublishedAndAStaleFileAtTheSamePlaceTrashOneAndDeleteTheOther() {
    PublishedFileEntry published = image("a.png");
    PublishedFileEntry stale = image("a.png").withStatus(PublishedFileStatus.STALE);
    PublishedObjectFiles stored = stored(page("index.php"), published, stale);

    PublishedObjectFilesChanges changes = publish(stored, page("index.php"));

    assertThat(changes.trashes()).as("trashes").containsExactly(published);
    assertThat(changes.deletes()).as("deletes").containsExactly(stale);
  }

  @Test
  void rejectsAFileOfAnotherChannel() {
    PublishedFileEntry foreign =
        page("index.php").toBuilder().key(PublishedFileKey.ofEntity("56", ENTITY)).build();

    assertThatThrownBy(() -> stored(foreign)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsChangesToAnotherChannel() {
    PublishedObjectFiles other = new PublishedObjectFiles("56", ENTITY, List.of());

    assertThatThrownBy(() -> stored().changesTo(other))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankIdentifiers() {
    assertThatThrownBy(() -> new PublishedObjectFiles(null, ENTITY, List.of()))
        .as("no channel")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PublishedObjectFiles(CHANNEL, " ", List.of()))
        .as("blank entity")
        .isInstanceOf(IllegalArgumentException.class);
  }
}
