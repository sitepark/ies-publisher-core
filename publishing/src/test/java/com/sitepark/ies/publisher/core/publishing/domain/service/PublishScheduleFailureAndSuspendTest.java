package com.sitepark.ies.publisher.core.publishing.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** Tasks that fail, telling that everything is done, and suspending publishing. */
@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class PublishScheduleFailureAndSuspendTest extends PublishScheduleTestSupport {

  // --- failures

  @Test
  void triesATaskAgainThatFailedForAPassingReason() {
    this.urgent("a");
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.RETRY);

    assertThat(this.schedule.pollNext(FOUR).orElseThrow().attempt())
        .as("the task is handed out again as a second attempt")
        .isEqualTo(1);
  }

  @Test
  void aListWaitsForTheSecondAttempt() {
    PublishListId id = this.urgent("a");
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.RETRY);

    assertThat(this.schedule.list(id).orElseThrow().isFinished())
        .as("the task is tried again")
        .isFalse();

    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.DONE);

    assertThat(this.schedule.list(id).orElseThrow().isComplete())
        .as("the second attempt did it")
        .isTrue();
  }

  @Test
  void givesUpAfterTheSecondAttempt() {
    this.urgent("a");
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.RETRY);
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.RETRY);

    assertThat(this.schedule.pollNext(FOUR)).as("there is no third attempt").isEmpty();
    assertThat(this.schedule.status().failed()).as("the task counts as failed").isEqualTo(1);
  }

  @Test
  void newerRequestReplacesTheSecondAttempt() {
    PublishListId first = this.urgent("a");
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();
    this.open(PublishPriority.URGENT, revoke("a"));
    this.schedule.completed(running, PublishOutcome.RETRY);

    PublishTask next = this.schedule.pollNext(FOUR).orElseThrow();

    assertThat(next.type())
        .as("the request made meanwhile is not overwritten by the retry")
        .isEqualTo(PublishTaskType.DEPUBLISH);
    assertThat(this.schedule.status().waiting()).as("the retry is dropped").isZero();

    this.schedule.completed(next, PublishOutcome.DONE);

    assertThat(this.schedule.list(first).orElseThrow().isFinished())
        .as("the list of the failed attempt is served by the newer request")
        .isTrue();
  }

  @Test
  void countsFailedTasks() {
    this.urgent("a");
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.FAILED);

    assertThat(this.schedule.status().failed()).as("the failure is counted").isEqualTo(1);
    assertThat(this.schedule.status().processed()).as("and the task is processed").isEqualTo(1);
  }

  // --- drained, suspended

  @Test
  void tellsOnceThatEverythingIsDone() throws InterruptedException {
    this.urgent("a");
    this.schedule.completed(this.schedule.awaitNext(() -> FOUR).orElseThrow(), PublishOutcome.DONE);

    assertThat(this.schedule.awaitNext(() -> FOUR)).as("nothing is left to do").isEmpty();

    CompletableFuture<Optional<PublishTask>> again =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                return this.schedule.awaitNext(() -> FOUR);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
              }
            });
    this.urgent("b");

    assertThat(again)
        .as("after that the dispatcher waits for new work instead of being told again")
        .succeedsWithin(Duration.ofSeconds(5))
        .matches(next -> next.isPresent() && "b".equals(next.get().key().entityId()));
  }

  @Test
  void handsOutNothingWhileSuspended() {
    this.urgent("a");
    this.schedule.suspend();

    assertThat(this.schedule.pollNext(FOUR)).as("nothing starts while suspended").isEmpty();

    this.schedule.resume();

    assertThat(this.next(FOUR)).as("publishing goes on afterwards").isEqualTo("a");
  }

  @Test
  void suspendingWaitsForWhatIsBeingPublished() {
    this.urgent("a");
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();

    CompletableFuture<Void> suspending = CompletableFuture.runAsync(this.schedule::suspend);
    await(() -> this.schedule.status().suspended());

    assertThat(suspending).as("suspending waits for the running task").isNotDone();

    this.schedule.completed(running, PublishOutcome.DONE);

    assertThat(suspending)
        .as("publishing is suspended once the task is done")
        .succeedsWithin(Duration.ofSeconds(5));
  }

  @Test
  void whoSuspendedMayStillPublishInTheForeground() {
    this.schedule.suspend();
    this.schedule.enterForeground(publish("a"));

    assertThat(this.schedule.status().foreground())
        .as("the suspending thread is not locked out by itself")
        .isEqualTo(1);
  }

  @Test
  void knowsWhetherATaskIsWaiting() {
    this.urgent("a");

    assertThat(this.schedule.isWaiting(key("a"), PublishTaskType.PUBLISH))
        .as("the publication is waiting")
        .isTrue();
    assertThat(this.schedule.isWaiting(key("a"), PublishTaskType.DEPUBLISH))
        .as("no revocation is waiting")
        .isFalse();
  }
}
