package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList;
import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListOrigin;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.service.PublishSchedule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class PublishInForegroundTest {

  private static final PublishTask WWW =
      PublishTask.of(new PublishTaskKey("55", "4711"), PublishTaskType.PUBLISH);

  private static final PublishTask PREVIEW =
      PublishTask.of(new PublishTaskKey("56", "4711"), PublishTaskType.PUBLISH);

  private static final PublishListRequest SAVING =
      PublishListRequest.of(
          "Saving article 4711",
          PublishListOrigin.EDITOR,
          PublishPriority.URGENT,
          List.of(WWW, PREVIEW));

  private PublishSchedule schedule;

  private PublishInForeground useCase;

  @BeforeEach
  void setUp() {
    this.schedule = new PublishSchedule();
    this.useCase = new PublishInForeground(this.schedule);
  }

  @Test
  void publishesTheTasksOfTheListInOrder() {
    List<PublishTask> published = new ArrayList<>();

    this.useCase.publish(SAVING, published::add);

    assertThat(published)
        .as("every task of the list is published on the calling thread")
        .containsExactly(WWW, PREVIEW);
  }

  @Test
  void countsAsForegroundWhilePublishing() {
    AtomicInteger foreground = new AtomicInteger();

    this.useCase.publish(SAVING, task -> foreground.set(this.schedule.status().foreground()));

    assertThat(foreground.get())
        .as("background publishing is held back while the publication runs")
        .isEqualTo(1);
  }

  @Test
  void endsTheForegroundAfterwards() {
    this.useCase.publish(SAVING, task -> {});

    assertThat(this.schedule.status().foreground())
        .as("the foreground is over once the publication returns")
        .isZero();
  }

  @Test
  void theListCanBeLookedAtAfterwards() {
    PublishListId id = this.useCase.publish(SAVING, task -> {});

    PublishList list = this.schedule.list(id).orElseThrow();

    assertThat(list.name()).as("the list is known by its name").isEqualTo("Saving article 4711");
    assertThat(list.done()).as("both channels were published").isEqualTo(2);
    assertThat(list.isComplete()).as("the list went through").isTrue();
  }

  @Test
  void takesOverTheWaitingTaskOfTheEntity() {
    this.schedule.open(
        PublishListRequest.of(
            "dependencies", PublishListOrigin.DEPENDENCY, PublishPriority.URGENT, List.of(WWW)));

    this.useCase.publish(SAVING, task -> {});

    assertThat(this.schedule.status().waiting())
        .as("what the foreground published is not published again")
        .isZero();
  }

  @Test
  void passesTheErrorOfThePublicationOn() {
    assertThatThrownBy(
            () ->
                this.useCase.publish(
                    SAVING,
                    task -> {
                      throw new IOException("disk full");
                    }))
        .as("the caller learns why publishing failed")
        .isInstanceOf(IOException.class);
  }

  @Test
  void endsTheListAfterAnError() {
    Throwable _ =
        catchThrowable(
            () ->
                this.useCase.publish(
                    SAVING,
                    task -> {
                      throw new IOException("disk full");
                    }));

    PublishList list = this.schedule.lists().get(0);

    assertThat(this.schedule.status().foreground())
        .as("a failed publication must not hold the entity or throttle the background")
        .isZero();
    assertThat(list.failed()).as("the task that broke is named").containsExactly(WWW);
    assertThat(list.cancelled()).as("the task after it was not started").isEqualTo(1);
    assertThat(list.isFinished()).as("the list is over").isTrue();
  }
}
