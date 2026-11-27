package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.service.PublishSchedule;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListOrigin;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.port.PublishQueueListener;
import com.sitepark.ies.publisher.core.publishing.port.PublishTaskExecutor;
import com.sitepark.ies.publisher.core.publishing.port.PublishWorkers;
import com.sitepark.ies.publisher.core.publishing.port.PublishingLimits;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DispatchPublishTasksTest {

  private static final PublishTask TASK =
      PublishTask.of(new PublishTaskKey("55", "4711"), PublishTaskType.PUBLISH);

  private static final PublishListRequest LIST =
      PublishListRequest.of(
          "test", PublishListOrigin.ADMINISTRATION, PublishPriority.URGENT, List.of(TASK));

  private PublishSchedule schedule;

  private PublishWorkers workers;

  private PublishTaskExecutor executor;

  private PublishQueueListener listener;

  private DispatchPublishTasks useCase;

  @BeforeEach
  void setUp() {
    this.schedule = new PublishSchedule();
    this.workers = mock();
    this.executor = mock();
    this.listener = mock();
    PublishingLimits limits = mock();
    when(limits.parallel()).thenReturn(2);
    // the worker of this test runs the work on the dispatching thread
    doAnswer(
            invocation -> {
              invocation.<Runnable>getArgument(0).run();
              return null;
            })
        .when(this.workers)
        .submit(any());
    this.useCase =
        new DispatchPublishTasks(this.schedule, limits, this.workers, this.executor, this.listener);
  }

  @Test
  void executesTheNextTaskOnAWorker() throws InterruptedException {
    when(this.executor.execute(TASK)).thenReturn(PublishOutcome.DONE);
    this.schedule.open(LIST);

    this.useCase.dispatchNext();

    verify(this.executor).execute(TASK);
  }

  @Test
  void reportsTheEndOfATask() throws InterruptedException {
    when(this.executor.execute(TASK)).thenReturn(PublishOutcome.DONE);
    this.schedule.open(LIST);

    this.useCase.dispatchNext();

    assertThat(this.schedule.status().running())
        .as("the entity is free again once the task is done")
        .isZero();
  }

  @Test
  void tellsTheListenerWhenEverythingIsDone() throws InterruptedException {
    when(this.executor.execute(TASK)).thenReturn(PublishOutcome.DONE);
    this.schedule.open(LIST);
    this.useCase.dispatchNext();

    this.useCase.dispatchNext();

    verify(this.listener).drained();
  }

  @Test
  void tellsTheListenerAboutATaskThatBreaks() throws InterruptedException {
    IllegalStateException broken = new IllegalStateException("broken");
    when(this.executor.execute(TASK)).thenThrow(broken);
    this.schedule.open(LIST);

    this.useCase.dispatchNext();

    verify(this.listener).failed(TASK, broken);
  }

  @Test
  void freesTheEntityOfATaskThatBreaks() throws InterruptedException {
    when(this.executor.execute(TASK)).thenThrow(new IllegalStateException("broken"));
    this.schedule.open(LIST);

    this.useCase.dispatchNext();

    assertThat(this.schedule.status().failed())
        .as("a broken task counts as failed and no longer holds its entity")
        .isEqualTo(1);
  }

  @Test
  void freesTheEntityOfATaskTheWorkersRefuse() throws InterruptedException {
    doThrow(new RejectedExecutionException("shut down")).when(this.workers).submit(any());
    this.schedule.open(LIST);

    this.useCase.dispatchNext();

    assertThat(this.schedule.status().running())
        .as("a task that never ran must not keep its entity taken")
        .isZero();
  }
}
