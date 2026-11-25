package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.service.PublishSchedule;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublishInForegroundTest {

  private static final PublishTask TASK =
      PublishTask.of(
          new PublishTaskKey("55", "4711"), PublishTaskType.PUBLISH, PublishPriority.DEPENDENCY);

  private PublishSchedule schedule;

  private PublishInForeground useCase;

  @BeforeEach
  void setUp() {
    this.schedule = new PublishSchedule();
    this.useCase = new PublishInForeground(this.schedule);
  }

  @Test
  void countsAsForegroundWhilePublishing() {
    AtomicInteger foreground = new AtomicInteger();

    this.useCase.publish(TASK, () -> foreground.set(this.schedule.status().foreground()));

    assertThat(foreground.get())
        .as("background publishing is held back while the publication runs")
        .isEqualTo(1);
  }

  @Test
  void endsTheForegroundAfterwards() {
    this.useCase.publish(TASK, () -> {});

    assertThat(this.schedule.status().foreground())
        .as("the foreground is over once the publication returns")
        .isZero();
  }

  @Test
  void takesOverTheWaitingTaskOfTheEntity() {
    this.schedule.offer(List.of(TASK));

    this.useCase.publish(TASK, () -> {});

    assertThat(this.schedule.status().waiting())
        .as("what the foreground published is not published again")
        .isZero();
  }

  @Test
  void passesTheErrorOfThePublicationOn() {
    assertThatThrownBy(
            () ->
                this.useCase.publish(
                    TASK,
                    () -> {
                      throw new IOException("disk full");
                    }))
        .as("the caller learns why publishing failed")
        .isInstanceOf(IOException.class);
  }

  @Test
  void endsTheForegroundAfterAnError() {
    Throwable _ =
        catchThrowable(
            () ->
                this.useCase.publish(
                    TASK,
                    () -> {
                      throw new IOException("disk full");
                    }));

    assertThat(this.schedule.status().foreground())
        .as("a failed publication must not hold the entity or throttle the background")
        .isZero();
  }
}
