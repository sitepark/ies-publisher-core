package com.sitepark.ies.publisher.core.publishing.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList;
import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskOptions;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishingCapacity;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** Publishing in the foreground, alone and as a list, next to the background. */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
class PublishScheduleForegroundTest extends PublishScheduleTestSupport {

  // --- foreground

  @Test
  void foregroundTakesOverTheWaitingTaskOfItsEntity() {
    this.urgent("a");
    this.schedule.enterForeground(publish("a"));
    this.schedule.leaveForeground(key("a"), true);

    assertThat(this.schedule.pollNext(FOUR))
        .as("what the foreground published needs no background task")
        .isEmpty();
  }

  @Test
  void foregroundLeavesATaskWaitingForAnotherList() {
    PublishListId moved = this.urgent("moved");
    this.after(moved, "a");
    this.schedule.enterForeground(publish("a"));
    this.schedule.leaveForeground(key("a"), true);

    assertThat(this.schedule.isWaiting(key("a"), PublishTaskType.PUBLISH))
        .as("the task waits for something the foreground could not know")
        .isTrue();
  }

  @Test
  void foregroundLeavesTheCleanupWaiting() {
    this.open(PublishPriority.URGENT, cleanup("a"));
    this.schedule.enterForeground(publish("a"));
    this.schedule.leaveForeground(key("a"), true);

    assertThat(this.schedule.status().waiting()).as("the cleanup is still due").isEqualTo(1);
  }

  @Test
  void fewerBackgroundTasksStartWhileTheForegroundPublishes() {
    this.urgent("a", "b");
    this.schedule.enterForeground(publish("x"));
    this.schedule.pollNext(FOUR);

    assertThat(this.schedule.pollNext(FOUR))
        .as("only one background task may run while a user waits")
        .isEmpty();

    this.schedule.leaveForeground(key("x"), true);

    assertThat(this.next(FOUR)).as("the full limit applies again").isEqualTo("b");
  }

  @Test
  void backgroundSkipsAnEntityPublishedInTheForeground() {
    this.schedule.enterForeground(publish("a"));
    this.urgent("a", "b");

    assertThat(this.next(new PublishingCapacity(4, 4)))
        .as("the entity in the foreground is skipped, not waited for")
        .isEqualTo("b");
  }

  @Test
  void foregroundMayPublishItsEntityAgainFromWithin() {
    this.schedule.enterForeground(publish("a"));
    this.schedule.enterForeground(publish("a"));
    this.schedule.leaveForeground(key("a"), true);

    assertThat(this.schedule.status().foreground())
        .as("the outer publication is still running")
        .isEqualTo(1);
  }

  @Test
  void foregroundWaitsForItsEntityBeingPublishedInTheBackground() {
    this.urgent("a");
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();

    CompletableFuture<Void> foreground =
        CompletableFuture.runAsync(() -> this.schedule.enterForeground(publish("a")));
    await(() -> this.schedule.status().foreground() == 1);

    assertThat(foreground).as("the foreground waits for the running task").isNotDone();

    this.schedule.completed(running, PublishOutcome.DONE);

    assertThat(foreground)
        .as("the foreground goes on once the task is done")
        .succeedsWithin(Duration.ofSeconds(5));
  }

  @Test
  void waitingForegroundGoesBeforeTheBackground() {
    this.urgent("a");
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();
    CompletableFuture<Void> foreground =
        CompletableFuture.runAsync(() -> this.schedule.enterForeground(publish("a")));
    await(() -> this.schedule.status().foreground() == 1);
    this.urgent("a");
    this.schedule.completed(running, PublishOutcome.DONE);

    assertThat(foreground)
        .as("the foreground gets the entity")
        .succeedsWithin(Duration.ofSeconds(5));
    assertThat(this.schedule.status().waiting())
        .as("and takes over the task requested meanwhile")
        .isZero();
  }

  @Test
  void foregroundPublishingLeavesAWaitingRevokeAlone() {
    this.open(PublishPriority.URGENT, revoke("a"));

    this.schedule.enterForeground(publish("a"));

    assertThat(this.schedule.isWaiting(key("a"), PublishTaskType.DEPUBLISH))
        .as("publishing in the foreground does not serve a request to revoke the entity")
        .isTrue();
  }

  @Test
  void foregroundLeavesAWaitingTaskAskingForMoreAlone() {
    this.open(PublishPriority.URGENT, publish("a").withOptions(new PublishTaskOptions(true, true)));

    this.schedule.enterForeground(publish("a"));

    assertThat(this.schedule.status().waiting())
        .as("the waiting task forces scaled images, the foreground task does not")
        .isEqualTo(1);
  }

  @Test
  void foregroundTakesOverAWaitingTaskAskingForLess() {
    this.urgent("a");

    this.schedule.enterForeground(publish("a").withOptions(new PublishTaskOptions(true, true)));

    assertThat(this.schedule.status().waiting())
        .as("the foreground task does everything the waiting task asked for")
        .isZero();
  }

  @Test
  void aListComingAfterAListWithACleanupDoesNotWaitForTheCleanup() {
    PublishListId first = this.open(PublishPriority.URGENT, publish("x"), cleanup("y"));
    this.after(first, "a", "b");
    PublishTask x = this.schedule.pollNext(FOUR).orElseThrow();
    this.schedule.completed(x, PublishOutcome.DONE);

    String one = this.next(FOUR);
    String two = this.next(FOUR);

    assertThat(List.of(one, two))
        .as("the cleanup runs last, so the list after it must not wait for it, and runs parallel")
        .containsExactlyInAnyOrder("a", "b");
  }

  @Test
  void aThreadInTheForegroundIsPublishing() {
    this.schedule.enterForeground(publish("a"));

    assertThat(this.schedule.isPublishing())
        .as("the thread is in the middle of a publication")
        .isTrue();
  }

  @Test
  void aWorkerExecutingATaskIsPublishing() {
    this.urgent("a");
    this.schedule.pollNext(FOUR);
    this.schedule.adopt(key("a"));

    assertThat(this.schedule.isPublishing())
        .as("the thread that adopted the task executes it")
        .isTrue();
  }

  @Test
  void aThreadDoingNothingIsNotPublishing() {
    this.urgent("a");
    this.schedule.pollNext(FOUR);

    assertThat(this.schedule.isPublishing())
        .as("a task run by another thread does not make this one a publishing thread")
        .isFalse();
  }

  // --- lists published in the foreground

  @Test
  void aForegroundListIsWatchedLikeAnyOther() {
    PublishListId id = this.schedule.openForeground(request(PublishPriority.URGENT, "a", "b"));
    this.schedule.enterForeground(publish("a"), id);

    PublishList list = this.schedule.list(id).orElseThrow();

    assertThat(list.running()).as("one task is being published").isEqualTo(1);
    assertThat(list.waiting()).as("the other one is still to come").isEqualTo(1);
  }

  @Test
  void aForegroundListIsOverOnceItsThreadPublishedEverything() {
    PublishListId id = this.schedule.openForeground(request(PublishPriority.URGENT, "a"));
    this.schedule.enterForeground(publish("a"), id);
    this.schedule.leaveForeground(key("a"), true);
    this.schedule.closeForeground(id);

    assertThat(this.schedule.list(id).orElseThrow().isComplete())
        .as("the one task was published")
        .isTrue();
  }

  @Test
  void tasksAForegroundListNeverGotToCountAsCancelled() {
    PublishListId id = this.schedule.openForeground(request(PublishPriority.URGENT, "a", "b"));
    this.schedule.enterForeground(publish("a"), id);
    this.schedule.leaveForeground(key("a"), false);
    this.schedule.closeForeground(id);

    PublishList list = this.schedule.list(id).orElseThrow();

    assertThat(list.failed()).as("the task that broke").hasSize(1);
    assertThat(list.cancelled()).as("the task after it was not started").isEqualTo(1);
    assertThat(list.isFinished()).as("the list is over").isTrue();
  }

  @Test
  void aListComingAfterAForegroundListWaitsForItsThread() {
    PublishListId saving = this.schedule.openForeground(request(PublishPriority.URGENT, "a"));
    this.after(saving, "linking");

    assertThat(this.schedule.pollNext(FOUR))
        .as("the foreground list is published by its own thread, nothing untangles that")
        .isEmpty();

    this.schedule.closeForeground(saving);

    assertThat(this.next(FOUR)).as("it runs once the foreground list is over").isEqualTo("linking");
  }
}
