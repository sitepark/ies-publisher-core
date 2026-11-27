package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.publishing.domain.service.PublishSchedule;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListOrigin;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRetentionSetting;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishingCapacity;
import com.sitepark.ies.publisher.core.publishing.port.AccessControl;
import com.sitepark.ies.publisher.core.publishing.port.PublishListRetentionSettingStore;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class PublishListManagementTest {

  private static final AccessControl ALLOWED = () -> true;

  private static final AccessControl DENIED = () -> false;

  @Test
  void storesAndReadsTheRetention() {
    Store store = new Store();

    new SetPublishListRetention(ALLOWED, store, new PublishSchedule())
        .set(new PublishListRetentionSetting(5, 2));

    assertThat(new GetPublishListRetention(ALLOWED, store).get())
        .as("what was set is what is read")
        .isEqualTo(new PublishListRetentionSetting(5, 2));
  }

  @Test
  void aRetentionSetAppliesToTheScheduleAtOnce() {
    PublishSchedule schedule = new PublishSchedule();
    EnqueuePublishTasks enqueue = new EnqueuePublishTasks(schedule);
    enqueue.enqueue(empty());
    PublishListId second = enqueue.enqueue(empty());

    new SetPublishListRetention(ALLOWED, new Store(), schedule)
        .set(new PublishListRetentionSetting(1, 2));

    assertThat(schedule.lists())
        .as("the lists beyond the new setting are forgotten")
        .extracting(com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList::id)
        .containsExactly(second);
  }

  @Test
  void initAppliesTheStoredRetention() {
    Store store = new Store();
    store.store(new PublishListRetentionSetting(1, 2));
    PublishSchedule schedule = new PublishSchedule();
    EnqueuePublishTasks enqueue = new EnqueuePublishTasks(schedule);
    enqueue.enqueue(empty());
    PublishListId second = enqueue.enqueue(empty());

    new InitPublishSchedule(store, schedule).init();

    assertThat(schedule.lists())
        .as("the schedule keeps as many lists as stored, not as the default")
        .extracting(com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList::id)
        .containsExactly(second);
  }

  @Test
  void refusesTheRetentionToAUserWithoutAccess() {
    Store store = new Store();
    PublishSchedule schedule = new PublishSchedule();

    assertThatThrownBy(() -> new SetPublishListRetention(DENIED, store, schedule).set(store.get()))
        .as("setting needs the right to manage publishing")
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> new GetPublishListRetention(DENIED, store).get())
        .as("reading needs it as well")
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void refusesValuesOutOfRange() {
    assertThatThrownBy(() -> new PublishListRetentionSetting(-1, 24))
        .as("a negative number of lists")
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                new PublishListRetentionSetting(
                    20, PublishListRetentionSetting.MAX_LIFETIME_HOURS + 1))
        .as("a lifetime above the maximum")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void stopsOneListOrAll() {
    PublishSchedule schedule = new PublishSchedule();
    EnqueuePublishTasks enqueue = new EnqueuePublishTasks(schedule);
    PublishListId first = enqueue.enqueue(list("a"));
    enqueue.enqueue(list("b"));
    StopPublishLists stop = new StopPublishLists(ALLOWED, schedule);

    assertThat(stop.stop(first)).as("the list was stopped").isTrue();
    assertThat(stop.stopAll()).as("one list was left to stop").isEqualTo(1);
    assertThat(schedule.status().waiting()).as("nothing waits any more").isZero();
  }

  @Test
  void refusesToStopListsForAUserWithoutAccess() {
    PublishSchedule schedule = new PublishSchedule();
    PublishListId id = new EnqueuePublishTasks(schedule).enqueue(list("a"));
    StopPublishLists stop = new StopPublishLists(DENIED, schedule);

    assertThatThrownBy(() -> stop.stop(id))
        .as("stopping a list needs the right to manage publishing")
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(stop::stopAll)
        .as("and so does stopping all of them")
        .isInstanceOf(AccessDeniedException.class);
    assertThat(schedule.status().waiting()).as("nothing was dropped").isEqualTo(1);
  }

  @Test
  void aListKnowsHowLongItTook() throws InterruptedException {
    PublishSchedule schedule = new PublishSchedule();
    PublishListId id = new EnqueuePublishTasks(schedule).enqueue(list("a"));
    assertThat(schedule.list(id).orElseThrow().duration())
        .as("no task has started, so there is no time yet")
        .isNull();

    PublishTask task = schedule.pollNext(new PublishingCapacity(1, 1)).orElseThrow();
    Thread.sleep(20);
    schedule.completed(
        task, com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome.DONE);

    assertThat(schedule.list(id).orElseThrow().duration())
        .as("the time from the start of the first task to the end of the last")
        .isGreaterThanOrEqualTo(java.time.Duration.ofMillis(15));
  }

  private static PublishListRequest empty() {
    return PublishListRequest.of(
        "empty", PublishListOrigin.ADMINISTRATION, PublishPriority.NORMAL, List.of());
  }

  private static PublishListRequest list(String entity) {
    return PublishListRequest.of(
        "list " + entity,
        PublishListOrigin.ADMINISTRATION,
        PublishPriority.NORMAL,
        List.of(PublishTask.of(new PublishTaskKey("1", entity), PublishTaskType.PUBLISH)));
  }

  private static final class Store implements PublishListRetentionSettingStore {

    private PublishListRetentionSetting setting = PublishListRetentionSetting.DEFAULT;

    @Override
    public PublishListRetentionSetting get() {
      return this.setting;
    }

    @Override
    public void store(PublishListRetentionSetting value) {
      this.setting = value;
    }
  }
}
