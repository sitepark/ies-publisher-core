package com.sitepark.ies.publisher.core.publishing.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList;
import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListOrigin;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRetentionSetting;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** Watching, waiting for and cancelling lists, and how long the ones over are kept. */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
class PublishScheduleListsTest extends PublishScheduleTestSupport {

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
    this.schedule.retain(new PublishListRetentionSetting(2, 1));
    this.urgent();
    PublishListId second = this.urgent();
    PublishListId third = this.urgent();

    assertThat(this.schedule.lists())
        .as("the oldest list over the number is forgotten")
        .extracting(PublishList::id)
        .containsExactly(third, second);
  }

  @Test
  void keepsTheListsWithinTheLifetime() {
    MovableClock clock = new MovableClock();
    this.schedule = new PublishSchedule(clock);
    this.schedule.retain(new PublishListRetentionSetting(20, 1));
    PublishListId id = this.urgent();
    clock.moveOn(Duration.ofMinutes(59));

    assertThat(this.schedule.lists())
        .as("a list over for less than the lifetime is kept")
        .extracting(PublishList::id)
        .containsExactly(id);
  }

  @Test
  void aNewRetentionAppliesAtOnce() {
    this.urgent();
    PublishListId second = this.urgent();

    this.schedule.retain(new PublishListRetentionSetting(1, 1));

    assertThat(this.schedule.lists())
        .as("the lists beyond the new setting are forgotten right away")
        .extracting(PublishList::id)
        .containsExactly(second);
  }

  @Test
  void keepsTwentyListsUntilToldOtherwise() {
    for (int i = 0; i < 21; i++) {
      this.urgent();
    }

    assertThat(this.schedule.lists())
        .as("a new schedule keeps as many lists as the default setting")
        .hasSize(PublishListRetentionSetting.DEFAULT.keptLists());
  }

  @Test
  void forgetsAListOnceItIsOverForLongerThanTheLifetime() {
    MovableClock clock = new MovableClock();
    this.schedule = new PublishSchedule(clock);
    this.schedule.retain(new PublishListRetentionSetting(20, 1));
    this.urgent();
    clock.moveOn(Duration.ofHours(1));

    assertThat(this.schedule.lists()).as("the list is too old").isEmpty();
  }

  @Test
  void doesNotFindAListThatIsOverForLongerThanTheLifetime() {
    MovableClock clock = new MovableClock();
    this.schedule = new PublishSchedule(clock);
    this.schedule.retain(new PublishListRetentionSetting(20, 1));
    PublishListId id = this.urgent();
    clock.moveOn(Duration.ofHours(1));

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
  void keepsAListThatIsStillGoingOnWhateverItsAge() {
    MovableClock clock = new MovableClock();
    this.schedule = new PublishSchedule(clock);
    this.schedule.retain(new PublishListRetentionSetting(0, 1));
    this.urgent("a");
    clock.moveOn(Duration.ofHours(2));

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
}
