package com.sitepark.ies.publisher.core.publishing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList;
import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListOrigin;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskOptions;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.port.PublishListRetention;
import com.sitepark.ies.publisher.core.publishing.port.PublishingLimits;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
@SuppressWarnings({
  "PMD.TooManyMethods",
  "PMD.UnitTestContainsTooManyAsserts",
  "PMD.CyclomaticComplexity"
})
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
    this.bulk("a");
    this.normal("b");
    this.urgent("c", "d");

    assertThat(this.next(FOUR)).as("the first urgent task goes first").isEqualTo("c");
    assertThat(this.next(FOUR)).as("the second urgent task follows").isEqualTo("d");
    assertThat(this.next(FOUR)).as("what is merely due comes next").isEqualTo("b");
    assertThat(this.next(FOUR)).as("bulk work comes last").isEqualTo("a");
  }

  @Test
  void handsOutNoMoreThanTheLimit() {
    this.urgent("a", "b");
    this.schedule.pollNext(ONE);

    assertThat(this.schedule.pollNext(ONE))
        .as("a second task must not start while the limit of one is used up")
        .isEmpty();
  }

  @Test
  void handsOutTheNextTaskOnceARunningOneIsDone() {
    this.urgent("a", "b");
    PublishTask first = this.schedule.pollNext(ONE).orElseThrow();
    this.schedule.completed(first, PublishOutcome.DONE);

    assertThat(this.next(ONE)).as("the limit is free again").isEqualTo("b");
  }

  // --- the same entity again

  @Test
  void mergesARequestIntoTheWaitingTaskOfTheEntity() {
    this.urgent("a", "b");
    this.urgent("a");

    assertThat(this.next(FOUR)).as("the entity keeps its place").isEqualTo("a");
    assertThat(this.next(FOUR)).as("the other entity follows").isEqualTo("b");
    assertThat(this.schedule.pollNext(FOUR)).as("the entity is published once").isEmpty();
  }

  @Test
  void mergedTaskIsAsUrgentAsTheMostUrgentList() {
    this.bulk("a");
    this.urgent("b", "a");

    assertThat(this.next(FOUR))
        .as("the bulk task an urgent list asked for as well keeps its earlier place")
        .isEqualTo("a");
  }

  @Test
  void mergedTaskNeverLosesUrgency() {
    this.urgent("a");
    this.urgent("b");
    this.bulk("a");

    assertThat(this.next(FOUR))
        .as("a bulk list asking for the entity too must not push it back")
        .isEqualTo("a");
  }

  @Test
  void mergedTaskDoesWhatEitherRequestAskedFor() {
    this.open(
        PublishPriority.URGENT,
        publish("a").withOptions(new PublishTaskOptions(false, false)),
        publish("a").withOptions(new PublishTaskOptions(false, true)),
        publish("a"));

    assertThat(this.schedule.pollNext(FOUR).orElseThrow().options())
        .as("options of all merged requests apply")
        .isEqualTo(new PublishTaskOptions(true, true));
  }

  @Test
  void youngerRequestDecidesBetweenPublishingAndRevoking() {
    this.open(PublishPriority.URGENT, publish("a"), revoke("a"));

    assertThat(this.schedule.pollNext(FOUR).orElseThrow().type())
        .as("revoking requested after publishing supersedes it")
        .isEqualTo(PublishTaskType.DEPUBLISH);
  }

  @Test
  void keepsTheAttachmentOfTheYoungerRequest() {
    this.open(
        PublishPriority.URGENT,
        publish("a").withAttachment("old"),
        publish("a").withAttachment("new"));

    assertThat(this.schedule.pollNext(FOUR).orElseThrow().attachment())
        .as("the younger request knows best what the executor needs")
        .isEqualTo("new");
  }

  @Test
  void doesNotPublishAnEntityTwiceAtOnce() {
    this.urgent("a");
    this.schedule.pollNext(FOUR);
    this.urgent("a");

    assertThat(this.schedule.pollNext(FOUR))
        .as("a request for an entity being published has to wait")
        .isEmpty();
  }

  @Test
  void publishesAnEntityAgainThatWasRequestedWhileItRan() {
    this.urgent("a");
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();
    this.urgent("a");
    this.urgent("a");
    this.schedule.completed(running, PublishOutcome.DONE);

    assertThat(this.next(FOUR)).as("the request made meanwhile runs afterwards").isEqualTo("a");
    assertThat(this.schedule.pollNext(FOUR)).as("and only once").isEmpty();
  }

  @Test
  void translationAndContentOfAnEntityAreSeparateTasks() {
    this.open(PublishPriority.URGENT, publish("a"), task("a", PublishTaskType.TRANSLATION));

    assertThat(this.schedule.status().waiting())
        .as("a translation is not merged into a publication")
        .isEqualTo(2);
  }

  // --- lists coming after other lists

  @Test
  void listWaitsForTheListItComesAfter() {
    PublishListId moved = this.urgent("moved");
    this.after(moved, "linking");
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();

    assertThat(running.key().entityId()).as("the list that comes first starts").isEqualTo("moved");
    assertThat(this.schedule.pollNext(FOUR))
        .as("the list coming after it must not start while it runs")
        .isEmpty();

    this.schedule.completed(running, PublishOutcome.DONE);

    assertThat(this.next(FOUR)).as("it runs once the first list is over").isEqualTo("linking");
  }

  @Test
  void listDoesNotWaitForListsItDoesNotComeAfter() {
    PublishListId moved = this.urgent("moved");
    this.after(moved, "linking");
    this.urgent("other");
    this.schedule.pollNext(FOUR);

    assertThat(this.next(FOUR))
        .as("a list requested later that waits for nothing overtakes the waiting one")
        .isEqualTo("other");
  }

  @Test
  void tasksOfAListComingAfterAnotherRunInParallel() {
    PublishListId moved = this.urgent("moved");
    this.after(moved, "a", "b");
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.DONE);
    this.schedule.pollNext(FOUR);

    assertThat(this.next(FOUR)).as("the tasks do not wait for each other").isEqualTo("b");
  }

  @Test
  void listDoesNotWaitForAListThatIsOver() {
    PublishListId moved = this.urgent("moved");
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.DONE);
    this.after(moved, "linking");

    assertThat(this.next(FOUR))
        .as("a list that is over is nothing to wait for")
        .isEqualTo("linking");
  }

  @Test
  void taskOfBothListsWaitsOnlyForTheOtherTasksOfTheFirst() {
    PublishListId moved = this.urgent("linking", "moved");
    this.after(moved, "linking");

    PublishTask first = this.schedule.pollNext(FOUR).orElseThrow();

    assertThat(first.key().entityId())
        .as("the task both lists asked for waits for the rest of the first list")
        .isEqualTo("moved");
    assertThat(this.schedule.pollNext(FOUR)).as("it still waits while that runs").isEmpty();

    this.schedule.completed(first, PublishOutcome.DONE);

    assertThat(this.next(FOUR)).as("then it runs").isEqualTo("linking");
    assertThat(this.schedule.pollNext(FOUR)).as("once").isEmpty();
  }

  @Test
  void listsWaitingForEachOtherAreUntangledByTheirOldestTask() {
    PublishListId first = this.urgent("a", "b");
    PublishListId second = this.after(first, "a");
    this.after(second, "b");

    assertThat(this.next(FOUR))
        .as("no order satisfies both lists: the oldest task runs so the rest can follow")
        .isEqualTo("a");
  }

  // --- cleanups

  @Test
  void cleanupWaitsUntilNothingElseWaitsOrRuns() {
    this.open(PublishPriority.URGENT, cleanup("a"), publish("b"));
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
    this.open(PublishPriority.URGENT, cleanup("a"), cleanup("b"));
    this.schedule.pollNext(FOUR);

    assertThat(this.schedule.pollNext(FOUR)).as("a cleanup runs alone").isEmpty();
  }

  @Test
  void cleanupWaitsForWorkRequestedWhileCleaningUp() {
    this.open(PublishPriority.URGENT, cleanup("a"), cleanup("b"));
    PublishTask first = this.schedule.pollNext(FOUR).orElseThrow();
    this.urgent("c");
    this.schedule.completed(first, PublishOutcome.DONE);

    assertThat(this.next(FOUR))
        .as("work requested meanwhile goes before the next cleanup")
        .isEqualTo("c");
  }

  @Test
  void nothingStartsInTheBackgroundWhileACleanupRuns() {
    this.open(PublishPriority.URGENT, cleanup("a"));
    this.schedule.pollNext(FOUR);
    this.urgent("b");

    assertThat(this.schedule.pollNext(FOUR))
        .as("a publication must not start while files are being deleted")
        .isEmpty();
  }

  @Test
  void cleanupsOfAnEntityAreMerged() {
    this.open(PublishPriority.URGENT, cleanup("a"), cleanup("a"));

    assertThat(this.schedule.status().waiting()).as("one cleanup per entity").isEqualTo(1);
  }

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

    assertThat(this.next(new Limits(4, 4)))
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

  // --- watching lists

  @Test
  void everyListGetsAnIdOfItsOwn() {
    PublishListId first = this.urgent("a");
    PublishListId second = this.urgent("b");

    assertThat(first).as("two lists never share an id").isNotEqualTo(second);
  }

  @Test
  void aListTellsWhatWasAskedFor() {
    PublishListId id =
        this.schedule.open(
            PublishListRequest.of(
                    "Dependencies of article 4711",
                    PublishListOrigin.DEPENDENCY,
                    PublishPriority.URGENT,
                    List.of(publish("a"), publish("b")))
                .causedBy("4711: url"));

    PublishList list = this.schedule.list(id).orElseThrow();

    assertThat(list.name()).as("the name given by the requester").contains("4711");
    assertThat(list.origin()).as("who asked").isEqualTo(PublishListOrigin.DEPENDENCY);
    assertThat(list.cause()).as("what led to the list").isEqualTo("4711: url");
    assertThat(list.total()).as("the tasks requested").isEqualTo(2);
    assertThat(list.waiting()).as("nothing has started yet").isEqualTo(2);
  }

  @Test
  void aListCountsItsRunningAndFinishedTasks() {
    PublishListId id = this.urgent("a", "b", "c");
    PublishTask first = this.schedule.pollNext(FOUR).orElseThrow();
    this.schedule.pollNext(FOUR);
    this.schedule.completed(first, PublishOutcome.DONE);

    PublishList list = this.schedule.list(id).orElseThrow();

    assertThat(list.done()).as("one task is done").isEqualTo(1);
    assertThat(list.running()).as("one is being carried out").isEqualTo(1);
    assertThat(list.waiting()).as("one is still waiting").isEqualTo(1);
    assertThat(list.isFinished()).as("the list is not over").isFalse();
  }

  @Test
  void aListIsOverOnceItsLastTaskIsDone() {
    PublishListId id = this.urgent("a");
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.DONE);

    PublishList list = this.schedule.list(id).orElseThrow();

    assertThat(list.isFinished()).as("no task is waiting or running").isTrue();
    assertThat(list.isComplete()).as("and all of them were carried out").isTrue();
  }

  @Test
  void aListWithoutTasksIsOverAtOnce() {
    PublishListId id = this.urgent();

    assertThat(this.schedule.list(id).orElseThrow().isFinished())
        .as("there is nothing to wait for")
        .isTrue();
  }

  @Test
  void keepsOnlyTheYoungestListsThatAreOver() {
    this.schedule = new PublishSchedule(new Retention(2, Duration.ofHours(1)));
    this.urgent();
    PublishListId second = this.urgent();
    PublishListId third = this.urgent();

    assertThat(this.schedule.lists())
        .as("the oldest list over the number is forgotten")
        .extracting(PublishList::id)
        .containsExactly(third, second);
  }

  @Test
  void forgetsAListOnceItIsOverForLongerThanTheLifetime() throws InterruptedException {
    this.schedule = new PublishSchedule(new Retention(20, Duration.ofMillis(20)));
    this.urgent();
    Thread.sleep(60);

    assertThat(this.schedule.lists()).as("the list is too old").isEmpty();
  }

  @Test
  void doesNotFindAListThatIsOverForLongerThanTheLifetime() throws InterruptedException {
    this.schedule = new PublishSchedule(new Retention(20, Duration.ofMillis(20)));
    PublishListId id = this.urgent();
    Thread.sleep(60);

    assertThat(this.schedule.list(id)).as("an expired list is not looked up any more").isEmpty();
    assertThatThrownBy(() -> this.schedule.await(id))
        .as("and is unknown to whoever waits for it")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void idsOfListsStaySortableByTheTimeTheyWereRequested() {
    PublishListId first = this.urgent();
    PublishListId second = this.urgent();

    assertThat(first.value())
        .as("fixed width, so text order is request order")
        .matches("\\d{8}-\\d{9}-\\d{12}");
    assertThat(first.value()).as("the list requested first sorts first").isLessThan(second.value());
  }

  @Test
  void keepsAListThatIsStillGoingOnWhateverItsAge() throws InterruptedException {
    this.schedule = new PublishSchedule(new Retention(0, Duration.ofMillis(1)));
    this.urgent("a");
    Thread.sleep(20);

    assertThat(this.schedule.lists()).as("a list that is not over is no matter of age").hasSize(1);
  }

  @Test
  void aListThatIsOverKeepsItsCountsButNotItsTasks() {
    PublishListId id = this.urgent("a", "b");
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.DONE);
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.DONE);

    PublishList list = this.schedule.list(id).orElseThrow();

    assertThat(list.total()).as("the number of tasks stays").isEqualTo(2);
    assertThat(list.done()).as("and how many are done").isEqualTo(2);
    assertThat(list.name()).as("and what describes the list").isNotBlank();
  }

  @Test
  void aTaskOfTwoListsCountsInBoth() {
    PublishListId first = this.urgent("a");
    PublishListId second = this.bulk("a");
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.DONE);

    assertThat(this.schedule.list(first).orElseThrow().done())
        .as("the one run serves the first list")
        .isEqualTo(1);
    assertThat(this.schedule.list(second).orElseThrow().done())
        .as("and the second one")
        .isEqualTo(1);
  }

  @Test
  void aListNamesTheTasksThatFailed() {
    PublishListId id = this.urgent("a");
    PublishTask task = this.schedule.pollNext(FOUR).orElseThrow();
    this.schedule.completed(task, PublishOutcome.FAILED);

    PublishList list = this.schedule.list(id).orElseThrow();

    assertThat(list.failed()).as("the failed task is reported").containsExactly(task);
    assertThat(list.isComplete()).as("the list did not go through").isFalse();
  }

  @Test
  void listsGoingOnComeBeforeListsThatAreOver() {
    PublishListId over = this.urgent("a");
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.DONE);
    PublishListId goingOn = this.urgent("b");

    assertThat(this.schedule.lists())
        .extracting(PublishList::id)
        .as("what is still being worked on is listed first")
        .containsExactly(goingOn, over);
  }

  @Test
  void anUnknownListIsNotFound() {
    assertThat(this.schedule.list(new PublishListId("nobody-000001")))
        .as("an id nobody was given names no list")
        .isEmpty();
  }

  // --- waiting for a list

  @Test
  void whoWaitsForAListGoesOnOnceItIsOver() {
    PublishListId id = this.bulk("a", "b");
    CompletableFuture<PublishList> waiting = this.awaitList(id);
    PublishTask first = this.schedule.pollNext(FOUR).orElseThrow();
    PublishTask second = this.schedule.pollNext(FOUR).orElseThrow();
    this.schedule.completed(first, PublishOutcome.DONE);

    assertThat(waiting).as("one task is still running").isNotDone();

    this.schedule.completed(second, PublishOutcome.DONE);

    assertThat(waiting)
        .as("both tasks are done")
        .succeedsWithin(Duration.ofSeconds(5))
        .matches(list -> list.done() == 2 && list.isComplete());
  }

  @Test
  void whoWaitsIsNotHeldUpByOtherLists() {
    PublishListId id = this.urgent("a");
    this.bulk("other");
    CompletableFuture<PublishList> waiting = this.awaitList(id);
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.DONE);

    assertThat(waiting)
        .as("the task of another list is still waiting, the own list is over")
        .succeedsWithin(Duration.ofSeconds(5));
  }

  @Test
  void taskForAnEntityBeingPublishedIsDoneAfterTheNextRun() {
    this.urgent("a");
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();
    PublishListId id = this.urgent("a");
    this.schedule.completed(running, PublishOutcome.DONE);

    assertThat(this.schedule.list(id).orElseThrow().isFinished())
        .as("the run that was under way may have read the old state")
        .isFalse();

    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.DONE);

    assertThat(this.schedule.list(id).orElseThrow().isFinished())
        .as("the run that followed serves the list")
        .isTrue();
  }

  @Test
  void taskTakenOverByTheForegroundIsDoneWithIt() {
    PublishListId id = this.urgent("a");
    this.schedule.enterForeground(publish("a"));

    assertThat(this.schedule.list(id).orElseThrow().running())
        .as("the foreground is publishing what the list asked for")
        .isEqualTo(1);

    this.schedule.leaveForeground(key("a"), true);

    assertThat(this.schedule.list(id).orElseThrow().done())
        .as("the foreground did what the list waited for")
        .isEqualTo(1);
  }

  @Test
  void waitingForAnUnknownListFails() {
    assertThatThrownBy(() -> this.schedule.await(new PublishListId("nobody-000001")))
        .as("nobody can wait for a list that was never requested")
        .isInstanceOf(IllegalArgumentException.class);
  }

  // --- cancelling

  @Test
  void cancellingAListDropsItsWaitingTasks() {
    PublishListId id = this.bulk("a", "b");

    this.schedule.cancel(id);

    PublishList list = this.schedule.list(id).orElseThrow();
    assertThat(this.schedule.pollNext(FOUR)).as("nothing of the list is published").isEmpty();
    assertThat(list.cancelled()).as("both tasks count as cancelled").isEqualTo(2);
    assertThat(list.isFinished()).as("the list is over").isTrue();
  }

  @Test
  void cancellingAListKeepsWhatAnotherListWants() {
    PublishListId cancelled = this.bulk("a", "b");
    PublishListId kept = this.bulk("a");

    this.schedule.cancel(cancelled);

    assertThat(this.next(FOUR)).as("the other list still gets its task").isEqualTo("a");
    assertThat(this.schedule.pollNext(FOUR)).as("and nothing else runs").isEmpty();
    assertThat(this.schedule.list(kept).orElseThrow().cancelled())
        .as("the other list was not cancelled")
        .isZero();
  }

  @Test
  void taskLeftByAnUrgentListFallsBackToTheUrgencyOfTheRest() {
    this.bulk("a");
    this.bulk("b");
    PublishListId urgent = this.urgent("b");

    this.schedule.cancel(urgent);

    assertThat(this.next(FOUR))
        .as("without the urgent list the task is bulk work again, in its old place")
        .isEqualTo("a");
  }

  @Test
  void cancellingAListFinishesWhatIsRunning() {
    PublishListId id = this.bulk("a", "b");
    PublishTask running = this.schedule.pollNext(FOUR).orElseThrow();

    this.schedule.cancel(id);

    assertThat(this.schedule.list(id).orElseThrow().isFinished())
        .as("the task being carried out is not cut off")
        .isFalse();

    this.schedule.completed(running, PublishOutcome.DONE);

    PublishList list = this.schedule.list(id).orElseThrow();
    assertThat(list.done()).as("the running task was finished").isEqualTo(1);
    assertThat(list.cancelled()).as("the waiting one was dropped").isEqualTo(1);
  }

  @Test
  void cancellingAllListsDropsEverythingWaiting() {
    this.bulk("a", "b");
    this.urgent("c");

    int cancelled = this.schedule.cancelAll();

    assertThat(cancelled).as("both lists are cancelled").isEqualTo(2);
    assertThat(this.schedule.status().waiting()).as("nothing is waiting any more").isZero();
  }

  @Test
  void cancellingAListThatIsOverDoesNothing() {
    PublishListId id = this.urgent("a");
    this.schedule.completed(this.schedule.pollNext(FOUR).orElseThrow(), PublishOutcome.DONE);

    assertThat(this.schedule.cancel(id)).as("there is nothing left to cancel").isFalse();
  }

  @Test
  void whoWaitsLearnsThatTheListWasCancelled() {
    PublishListId id = this.bulk("a", "b");
    CompletableFuture<PublishList> waiting = this.awaitList(id);

    this.schedule.cancel(id);

    assertThat(waiting)
        .as("a cancelled list does not keep its requester waiting")
        .succeedsWithin(Duration.ofSeconds(5))
        .matches(list -> list.cancelled() == 2 && !list.isComplete());
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

  // --- helpers

  private String next(PublishingLimits limits) {
    return this.schedule.pollNext(limits).map(task -> task.key().entityId()).orElse("nothing");
  }

  private PublishListId urgent(String... entities) {
    return this.schedule.open(request(PublishPriority.URGENT, entities));
  }

  private PublishListId normal(String... entities) {
    return this.schedule.open(request(PublishPriority.NORMAL, entities));
  }

  private PublishListId bulk(String... entities) {
    return this.schedule.open(request(PublishPriority.BULK, entities));
  }

  /** An urgent list that comes after another one. */
  private PublishListId after(PublishListId predecessor, String... entities) {
    return this.schedule.open(request(PublishPriority.URGENT, entities).after(Set.of(predecessor)));
  }

  private PublishListId open(PublishPriority priority, PublishTask... tasks) {
    return this.schedule.open(
        PublishListRequest.of("test", PublishListOrigin.ADMINISTRATION, priority, List.of(tasks)));
  }

  private CompletableFuture<PublishList> awaitList(PublishListId id) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return this.schedule.await(id);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
          }
        });
  }

  private static PublishListRequest request(PublishPriority priority, String... entities) {
    List<PublishTask> tasks = new ArrayList<>();
    for (String entity : entities) {
      tasks.add(publish(entity));
    }
    return PublishListRequest.of("test", PublishListOrigin.ADMINISTRATION, priority, tasks);
  }

  private static PublishTaskKey key(String entity) {
    return new PublishTaskKey("55", entity);
  }

  private static PublishTask task(String entity, PublishTaskType type) {
    return PublishTask.of(key(entity), type);
  }

  private static PublishTask publish(String entity) {
    return task(entity, PublishTaskType.PUBLISH);
  }

  private static PublishTask revoke(String entity) {
    return task(entity, PublishTaskType.DEPUBLISH);
  }

  private static PublishTask cleanup(String entity) {
    return task(entity, PublishTaskType.CLEANUP);
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

  private record Retention(int keptLists, Duration lifetime) implements PublishListRetention {}

  private record Limits(int parallel, int parallelWhileInteractive) implements PublishingLimits {}
}
