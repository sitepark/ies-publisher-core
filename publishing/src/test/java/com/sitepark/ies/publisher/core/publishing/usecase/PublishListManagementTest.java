package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListOrigin;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRetentionSetting;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.port.AccessControl;
import com.sitepark.ies.publisher.core.publishing.port.PublishListRetentionSettingStore;
import com.sitepark.ies.publisher.core.publishing.service.PublishSchedule;
import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class PublishListManagementTest {

  private static final AccessControl ALLOWED = () -> true;

  private static final AccessControl DENIED = () -> false;

  @Test
  void storesAndReadsTheRetention() {
    Store store = new Store();

    new SetPublishListRetention(ALLOWED, store).set(new PublishListRetentionSetting(5, 2));

    assertThat(new GetPublishListRetention(ALLOWED, store).get())
        .as("what was set is what is read")
        .isEqualTo(new PublishListRetentionSetting(5, 2));
  }

  @Test
  void refusesTheRetentionToAUserWithoutAccess() {
    Store store = new Store();

    assertThatThrownBy(() -> new SetPublishListRetention(DENIED, store).set(store.get()))
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
    StopPublishLists stop = new StopPublishLists(ALLOWED, new CancelPublishLists(schedule));

    assertThat(stop.stop(first)).as("the list was stopped").isTrue();
    assertThat(stop.stopAll()).as("one list was left to stop").isEqualTo(1);
    assertThat(schedule.status().waiting()).as("nothing waits any more").isZero();
  }

  @Test
  void refusesToStopListsForAUserWithoutAccess() {
    PublishSchedule schedule = new PublishSchedule();
    PublishListId id = new EnqueuePublishTasks(schedule).enqueue(list("a"));
    StopPublishLists stop = new StopPublishLists(DENIED, new CancelPublishLists(schedule));

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

    PublishTask task = schedule.pollNext(new Limits()).orElseThrow();
    Thread.sleep(20);
    schedule.completed(
        task, com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome.DONE);

    assertThat(schedule.list(id).orElseThrow().duration())
        .as("the time from the start of the first task to the end of the last")
        .isGreaterThanOrEqualTo(java.time.Duration.ofMillis(15));
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

  private static final class Limits
      implements com.sitepark.ies.publisher.core.publishing.port.PublishingLimits {

    @Override
    public int parallel() {
      return 1;
    }

    @Override
    public int parallelWhileInteractive() {
      return 1;
    }
  }
}
