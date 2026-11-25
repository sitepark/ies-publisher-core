package com.sitepark.ies.publisher.core.publishing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskOptions;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.port.PublishingLimits;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
class PublishScheduleTest {

  private static final PublishingLimits FOUR = new Limits(4, 1);

  private static final PublishingLimits ONE = new Limits(1, 1);

  private PublishSchedule schedule;

  @BeforeEach
  void setUp() {
    this.schedule = new PublishSchedule();
  }

  // --- order and limits

  @Test
  void handsOutByPriorityThenByRequestOrder() {
    this.schedule.offer(List.of(bulk("a"), dependency("b"), dependency("c")));

    assertThat(this.next(FOUR)).as("the first dependency goes first").isEqualTo("b");
    assertThat(this.next(FOUR)).as("the second dependency follows").isEqualTo("c");
    assertThat(this.next(FOUR)).as("bulk work comes last").isEqualTo("a");
  }

  @Test
  void handsOutNoMoreThanTheLimit() {
    this.schedule.offer(List.of(dependency("a"), dependency("b")));
    this.schedule.pollNext(ONE);

    assertThat(this.schedule.pollNext(ONE))
        .as("a second task must not start while the limit of one is used up")
        .isEmpty();
  }

  @Test
  void handsOutTheNextTaskOnceARunningOneIsDone() {
    this.schedule.offer(List.of(dependency("a"), dependency("b")));
    PublishTask first = this.schedule.pollNext(ONE).orElseThrow();
    this.schedule.completed(first, PublishOutcome.DONE);

    assertThat(this.next(ONE)).as("the limit is free again").isEqualTo("b");
  }

  // --- the same entity again

  @Test
  void mergesARequestIntoTheWaitingTaskOfTheEntity() {
    this.schedule.offer(List.of(dependency("a"), dependency("b"), dependency("a")));

    assertThat(this.next(FOUR)).as("the entity keeps its place").isEqualTo("a");
    assertThat(this.next(FOUR)).as("the other entity follows").isEqualTo("b");
    assertThat(this.schedule.pollNext(FOUR)).as("the entity is published once").isEmpty();
  }

  @Test
  void mergedTaskTakesTheHigherPriority() {
    this.schedule.offer(List.of(bulk("a"), dependency("b"), dependency("a")));

    PublishTask first = this.schedule.pollNext(FOUR).orElseThrow();

    assertThat(first.key().entityId())
        .as("the bulk task raised to a dependency keeps its earlier place")
        .isEqualTo("a");
    assertThat(first.priority())
        .as("the merged task is as urgent as the more urgent request")
        .isEqualTo(PublishPriority.DEPENDENCY);
  }

  @Test
  void mergedTaskNeverLosesPriority() {
    this.schedule.offer(List.of(dependency("a"), bulk("a")));

    assertThat(this.schedule.pollNext(FOUR).orElseThrow().priority())
        .as("a bulk request must not lower a waiting dependency")
        .isEqualTo(PublishPriority.DEPENDENCY);
  }

  @Test
  void mergedTaskDoesWhatEitherRequestAskedFor() {
    PublishTask quiet = dependency("a").withOptions(new PublishTaskOptions(false, false));
    PublishTask scaling = dependency("a").withOptions(new PublishTaskOptions(false, true));
    this.schedule.offer(List.of(quiet, scaling, dependency("a")));

    assertThat(this.schedule.pollNext(FOUR).orElseThrow().options())
        .as("options of all merged requests apply")
        .isEqualTo(new PublishTaskOptions(true, true));
  }

  @Test
  void youngerRequestDecidesBetweenPublishingAndRevoking() {
    this.schedule.offer(
        List.of(dependency("a"), task("a", PublishTaskType.DEPUBLISH, PublishPriority.DEPENDENCY)));

    assertThat(this.schedule.pollNext(FOUR).orElseThrow().type())
        .as("revoking requested after publishing supersedes it")
        .isEqualTo(PublishTaskType.DEPUBLISH);
  }

  @Test
  void keepsTheAttachmentOfTheYoungerRequest() {
    this.schedule.offer(
        List.of(dependency("a").withAttachment("old"), dependency("a").withAttachment("new")));

    assertThat(this.schedule.pollNext(FOUR).orElseThrow().attachment())
        .as("the younger request knows best what the executor needs")
        .isEqualTo("new");
  }

  @Test
  void doesNotPublishAnEntityTwiceAtOnce() {
    this.schedule.offer(List.of(dependency("a")));
    this.schedule.pollNext(FOUR);
    this.schedule.offer(List.of(dependency("a")));

    assertThat(this.schedule.pollNext(FOUR))
        .as("a request for an entity being published has to wait")
        .isEmpty();
  }

  @Test
  void publishesAnEntityAgainThatWasRequestedWhileItRan() {
    this.schedule.offer(List.of(dependency("a")));
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();
    this.schedule.offer(List.of(dependency("a"), dependency("a")));
    this.schedule.completed(running, PublishOutcome.DONE);

    assertThat(this.next(FOUR)).as("the request made meanwhile runs afterwards").isEqualTo("a");
    assertThat(this.schedule.pollNext(FOUR)).as("and only once").isEmpty();
  }

  @Test
  void translationAndContentOfAnEntityAreSeparateTasks() {
    this.schedule.offer(
        List.of(
            dependency("a"), task("a", PublishTaskType.TRANSLATION, PublishPriority.DEPENDENCY)));

    assertThat(this.schedule.status().waiting())
        .as("a translation is not merged into a publication")
        .isEqualTo(2);
  }

  // --- follow-ups

  @Test
  void followUpWaitsForTasksRequestedBeforeIt() {
    this.schedule.offer(List.of(dependency("moved"), dependency("linking").asFollowUp()));
    PublishTask moved = this.schedule.pollNext(FOUR).orElseThrow();

    assertThat(this.schedule.pollNext(FOUR))
        .as("the follow-up must not run while the task before it runs")
        .isEmpty();

    this.schedule.completed(moved, PublishOutcome.DONE);

    assertThat(this.next(FOUR)).as("the follow-up runs afterwards").isEqualTo("linking");
  }

  @Test
  void followUpDoesNotWaitForLessUrgentTasks() {
    this.schedule.offer(List.of(bulk("site"), dependency("linking").asFollowUp()));

    assertThat(this.next(FOUR))
        .as("bulk work requested earlier does not hold a dependency back")
        .isEqualTo("linking");
  }

  @Test
  void followUpsRunInParallel() {
    this.schedule.offer(List.of(dependency("a").asFollowUp(), dependency("b").asFollowUp()));
    this.schedule.pollNext(FOUR);

    assertThat(this.next(FOUR)).as("follow-ups do not wait for each other").isEqualTo("b");
  }

  @Test
  void tasksRequestedLaterOvertakeAWaitingFollowUp() {
    this.schedule.offer(
        List.of(dependency("moved"), dependency("linking").asFollowUp(), dependency("other")));
    this.schedule.pollNext(FOUR);

    assertThat(this.next(FOUR))
        .as("a task that waits for nothing runs while the follow-up waits")
        .isEqualTo("other");
  }

  @Test
  void entityRequestedPlainAndAsFollowUpIsPublishedOnceAndNotTooEarly() {
    this.schedule.offer(
        List.of(dependency("linking"), dependency("moved"), dependency("linking").asFollowUp()));

    PublishTask moved = this.schedule.pollNext(FOUR).orElseThrow();

    assertThat(moved.key().entityId())
        .as("the merged task waits although it was requested first")
        .isEqualTo("moved");
    assertThat(this.schedule.pollNext(FOUR)).as("it still waits while that runs").isEmpty();

    this.schedule.completed(moved, PublishOutcome.DONE);

    assertThat(this.next(FOUR)).as("then it runs").isEqualTo("linking");
    assertThat(this.schedule.pollNext(FOUR)).as("once").isEmpty();
  }

  // --- cleanups

  @Test
  void cleanupWaitsUntilNothingElseWaitsOrRuns() {
    this.schedule.offer(List.of(cleanup("a"), dependency("b")));
    PublishTask publishing = this.schedule.pollNext(FOUR).orElseThrow();

    assertThat(publishing.key().entityId()).as("publishing goes first").isEqualTo("b");
    assertThat(this.schedule.pollNext(FOUR))
        .as("the cleanup must not run while something is published")
        .isEmpty();

    this.schedule.completed(publishing, PublishOutcome.DONE);

    assertThat(this.schedule.pollNext(FOUR).orElseThrow().type())
        .as("the cleanup runs once everything is done")
        .isEqualTo(PublishTaskType.CLEANUP);
  }

  @Test
  void cleanupsRunOneAtATime() {
    this.schedule.offer(List.of(cleanup("a"), cleanup("b")));
    this.schedule.pollNext(FOUR);

    assertThat(this.schedule.pollNext(FOUR)).as("a cleanup runs alone").isEmpty();
  }

  @Test
  void cleanupWaitsForWorkRequestedWhileCleaningUp() {
    this.schedule.offer(List.of(cleanup("a"), cleanup("b")));
    PublishTask first = this.schedule.pollNext(FOUR).orElseThrow();
    this.schedule.offer(List.of(dependency("c")));
    this.schedule.completed(first, PublishOutcome.DONE);

    assertThat(this.next(FOUR))
        .as("work requested meanwhile goes before the next cleanup")
        .isEqualTo("c");
  }

  @Test
  void nothingStartsInTheBackgroundWhileACleanupRuns() {
    this.schedule.offer(List.of(cleanup("a")));
    this.schedule.pollNext(FOUR);
    this.schedule.offer(List.of(dependency("b")));

    assertThat(this.schedule.pollNext(FOUR))
        .as("a publication must not start while files are being deleted")
        .isEmpty();
  }

  @Test
  void cleanupsOfAnEntityAreMerged() {
    this.schedule.offer(List.of(cleanup("a"), cleanup("a")));

    assertThat(this.schedule.status().waiting()).as("one cleanup per entity").isEqualTo(1);
  }

  // --- foreground

  @Test
  void foregroundTakesOverTheWaitingTaskOfItsEntity() {
    this.schedule.offer(List.of(dependency("a")));
    this.schedule.enterForeground(dependency("a"));
    this.schedule.leaveForeground(key("a"));

    assertThat(this.schedule.pollNext(FOUR))
        .as("what the foreground published needs no background task")
        .isEmpty();
  }

  @Test
  void foregroundLeavesAFollowUpWaiting() {
    this.schedule.offer(List.of(dependency("a").asFollowUp()));
    this.schedule.enterForeground(dependency("a"));
    this.schedule.leaveForeground(key("a"));

    assertThat(this.next(FOUR))
        .as("the follow-up waits for something the foreground could not know")
        .isEqualTo("a");
  }

  @Test
  void foregroundLeavesTheCleanupWaiting() {
    this.schedule.offer(List.of(cleanup("a")));
    this.schedule.enterForeground(dependency("a"));
    this.schedule.leaveForeground(key("a"));

    assertThat(this.schedule.status().waiting()).as("the cleanup is still due").isEqualTo(1);
  }

  @Test
  void fewerBackgroundTasksStartWhileTheForegroundPublishes() {
    this.schedule.offer(List.of(dependency("a"), dependency("b")));
    this.schedule.enterForeground(dependency("x"));
    this.schedule.pollNext(FOUR);

    assertThat(this.schedule.pollNext(FOUR))
        .as("only one background task may run while a user waits")
        .isEmpty();

    this.schedule.leaveForeground(key("x"));

    assertThat(this.next(FOUR)).as("the full limit applies again").isEqualTo("b");
  }

  @Test
  void backgroundSkipsAnEntityPublishedInTheForeground() {
    this.schedule.enterForeground(dependency("a"));
    this.schedule.offer(List.of(dependency("a"), dependency("b")));

    assertThat(this.next(new Limits(4, 4)))
        .as("the entity in the foreground is skipped, not waited for")
        .isEqualTo("b");
  }

  @Test
  void foregroundMayPublishItsEntityAgainFromWithin() {
    this.schedule.enterForeground(dependency("a"));
    this.schedule.enterForeground(dependency("a"));
    this.schedule.leaveForeground(key("a"));

    assertThat(this.schedule.status().foreground())
        .as("the outer publication is still running")
        .isEqualTo(1);
  }

  @Test
  void foregroundWaitsForItsEntityBeingPublishedInTheBackground() {
    this.schedule.offer(List.of(dependency("a")));
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();

    CompletableFuture<Void> foreground =
        CompletableFuture.runAsync(() -> this.schedule.enterForeground(dependency("a")));
    await(() -> this.schedule.status().foreground() == 1);

    assertThat(foreground).as("the foreground waits for the running task").isNotDone();

    this.schedule.completed(running, PublishOutcome.DONE);

    assertThat(foreground)
        .as("the foreground goes on once the task is done")
        .succeedsWithin(Duration.ofSeconds(5));
  }

  @Test
  void waitingForegroundGoesBeforeTheBackground() {
    this.schedule.offer(List.of(dependency("a")));
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();
    CompletableFuture<Void> foreground =
        CompletableFuture.runAsync(() -> this.schedule.enterForeground(dependency("a")));
    await(() -> this.schedule.status().foreground() == 1);
    this.schedule.offer(List.of(dependency("a")));
    this.schedule.completed(running, PublishOutcome.DONE);

    assertThat(foreground)
        .as("the foreground gets the entity")
        .succeedsWithin(Duration.ofSeconds(5));
    assertThat(this.schedule.status().waiting())
        .as("and takes over the task requested meanwhile")
        .isZero();
  }

  // --- failures

  @Test
  void triesATaskAgainThatFailedForAPassingReason() {
    this.schedule.offer(List.of(dependency("a")));
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.RETRY);

    assertThat(this.schedule.pollNext(FOUR).orElseThrow().attempt())
        .as("the task is handed out again as a second attempt")
        .isEqualTo(1);
  }

  @Test
  void givesUpAfterTheSecondAttempt() {
    this.schedule.offer(List.of(dependency("a")));
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.RETRY);
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.RETRY);

    assertThat(this.schedule.pollNext(FOUR)).as("there is no third attempt").isEmpty();
    assertThat(this.schedule.status().failed()).as("the task counts as failed").isEqualTo(1);
  }

  @Test
  void newerRequestReplacesTheSecondAttempt() {
    this.schedule.offer(List.of(dependency("a")));
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();
    this.schedule.offer(List.of(task("a", PublishTaskType.DEPUBLISH, PublishPriority.DEPENDENCY)));
    this.schedule.completed(running, PublishOutcome.RETRY);

    PublishTask next = this.schedule.pollNext(FOUR).orElseThrow();

    assertThat(next.type())
        .as("the request made meanwhile is not overwritten by the retry")
        .isEqualTo(PublishTaskType.DEPUBLISH);
    assertThat(this.schedule.status().waiting()).as("the retry is dropped").isZero();
  }

  @Test
  void countsFailedTasks() {
    this.schedule.offer(List.of(dependency("a")));
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.FAILED);

    assertThat(this.schedule.status().failed()).as("the failure is counted").isEqualTo(1);
    assertThat(this.schedule.status().processed()).as("and the task is processed").isEqualTo(1);
  }

  // --- drained, suspended, cleared

  @Test
  void tellsOnceThatEverythingIsDone() throws InterruptedException {
    this.schedule.offer(List.of(dependency("a")));
    this.schedule.completed(this.schedule.awaitNext(FOUR).orElseThrow(), PublishOutcome.DONE);

    assertThat(this.schedule.awaitNext(FOUR)).as("nothing is left to do").isEmpty();

    CompletableFuture<Optional<PublishTask>> again =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                return this.schedule.awaitNext(FOUR);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
              }
            });
    this.schedule.offer(List.of(dependency("b")));

    assertThat(again)
        .as("after that the dispatcher waits for new work instead of being told again")
        .succeedsWithin(Duration.ofSeconds(5))
        .matches(next -> next.isPresent() && "b".equals(next.get().key().entityId()));
  }

  @Test
  void handsOutNothingWhileSuspended() {
    this.schedule.offer(List.of(dependency("a")));
    this.schedule.suspend();

    assertThat(this.schedule.pollNext(FOUR)).as("nothing starts while suspended").isEmpty();

    this.schedule.resume();

    assertThat(this.next(FOUR)).as("publishing goes on afterwards").isEqualTo("a");
  }

  @Test
  void suspendingWaitsForWhatIsBeingPublished() {
    this.schedule.offer(List.of(dependency("a")));
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
    this.schedule.enterForeground(dependency("a"));

    assertThat(this.schedule.status().foreground())
        .as("the suspending thread is not locked out by itself")
        .isEqualTo(1);
  }

  @Test
  void clearingDropsWaitingTasks() {
    this.schedule.offer(List.of(dependency("a"), cleanup("b")));
    this.schedule.clear();

    assertThat(this.schedule.status().waiting()).as("nothing is waiting any more").isZero();
  }

  @Test
  void knowsWhetherATaskIsWaiting() {
    this.schedule.offer(List.of(dependency("a")));

    assertThat(this.schedule.isWaiting(key("a"), PublishTaskType.PUBLISH))
        .as("the publication is waiting")
        .isTrue();
    assertThat(this.schedule.isWaiting(key("a"), PublishTaskType.DEPUBLISH))
        .as("no revocation is waiting")
        .isFalse();
  }

  // --- helpers

  private String next(PublishingLimits limits) {
    return this.schedule.pollNext(limits).map(task -> task.key().entityId()).orElse("nothing");
  }

  private static PublishTaskKey key(String entity) {
    return new PublishTaskKey("55", entity);
  }

  private static PublishTask task(String entity, PublishTaskType type, PublishPriority priority) {
    return PublishTask.of(key(entity), type, priority);
  }

  private static PublishTask dependency(String entity) {
    return task(entity, PublishTaskType.PUBLISH, PublishPriority.DEPENDENCY);
  }

  private static PublishTask bulk(String entity) {
    return task(entity, PublishTaskType.PUBLISH, PublishPriority.BULK);
  }

  private static PublishTask cleanup(String entity) {
    return task(entity, PublishTaskType.CLEANUP, PublishPriority.DEPENDENCY);
  }

  private static void await(BooleanSupplier condition) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("condition not reached in time");
      }
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AssertionError("interrupted while waiting", e);
      }
    }
  }

  private record Limits(int parallel, int parallelWhileInteractive) implements PublishingLimits {}
}
