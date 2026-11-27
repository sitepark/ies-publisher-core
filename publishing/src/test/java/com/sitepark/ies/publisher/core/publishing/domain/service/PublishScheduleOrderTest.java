package com.sitepark.ies.publisher.core.publishing.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskOptions;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import org.junit.jupiter.api.Test;

/** The order tasks are handed out in: priority, merging, lists coming after others, cleanups. */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.UnitTestContainsTooManyAsserts"})
class PublishScheduleOrderTest extends PublishScheduleTestSupport {

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
}
