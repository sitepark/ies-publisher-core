package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.service.PublishSchedule;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListOrigin;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class CancelPublishListsTest {

  private PublishSchedule schedule;

  private EnqueuePublishTasks enqueue;

  private GetPublishLists lists;

  private CancelPublishLists useCase;

  @BeforeEach
  void setUp() {
    this.schedule = new PublishSchedule();
    this.enqueue = new EnqueuePublishTasks(this.schedule);
    this.lists = new GetPublishLists(this.schedule);
    this.useCase = new CancelPublishLists(this.schedule);
  }

  @Test
  void cancelsOneList() {
    PublishListId cancelled = this.enqueue.enqueue(list("full generation", "a", "b"));
    PublishListId kept = this.enqueue.enqueue(list("dependencies", "c"));

    this.useCase.cancel(cancelled);

    assertThat(this.lists.get(cancelled).orElseThrow().cancelled())
        .as("the tasks of the cancelled list are dropped")
        .isEqualTo(2);
    assertThat(this.lists.get(kept).orElseThrow().waiting())
        .as("the other list goes on")
        .isEqualTo(1);
  }

  @Test
  void cancelsEveryList() {
    this.enqueue.enqueue(list("full generation", "a", "b"));
    this.enqueue.enqueue(list("dependencies", "c"));

    assertThat(this.useCase.cancelAll()).as("both lists are cancelled").isEqualTo(2);
    assertThat(this.schedule.status().waiting()).as("nothing is waiting any more").isZero();
  }

  @Test
  void tellsWhenThereIsNothingToCancel() {
    assertThat(this.useCase.cancel(new PublishListId("nobody-000001")))
        .as("an unknown list cannot be cancelled")
        .isFalse();
  }

  private static PublishListRequest list(String name, String... entities) {
    List<PublishTask> tasks =
        List.of(entities).stream()
            .map(e -> PublishTask.of(new PublishTaskKey("55", e), PublishTaskType.PUBLISH))
            .toList();
    return PublishListRequest.of(
        name, PublishListOrigin.ADMINISTRATION, PublishPriority.BULK, tasks);
  }
}
