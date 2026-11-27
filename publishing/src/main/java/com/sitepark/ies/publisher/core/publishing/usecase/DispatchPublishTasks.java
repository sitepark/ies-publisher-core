package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.service.PublishSchedule;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishOutcome;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishingCapacity;
import com.sitepark.ies.publisher.core.publishing.port.PublishQueueListener;
import com.sitepark.ies.publisher.core.publishing.port.PublishTaskExecutor;
import com.sitepark.ies.publisher.core.publishing.port.PublishWorkers;
import com.sitepark.ies.publisher.core.publishing.port.PublishingLimits;
import jakarta.inject.Inject;
import java.util.Optional;

/**
 * Hands the waiting tasks to the workers, one call per task. Meant to be called in a loop by one
 * dispatching thread.
 */
public class DispatchPublishTasks {

  private final PublishSchedule schedule;

  private final PublishingLimits limits;

  private final PublishWorkers workers;

  private final PublishTaskExecutor executor;

  private final PublishQueueListener listener;

  /**
   * Creates the use case.
   *
   * @param schedule what is published when
   * @param limits how much may run at the same time
   * @param workers the threads the tasks run on
   * @param executor what carries a task out
   * @param listener who is told when everything is done or a task breaks
   */
  @Inject
  public DispatchPublishTasks(
      PublishSchedule schedule,
      PublishingLimits limits,
      PublishWorkers workers,
      PublishTaskExecutor executor,
      PublishQueueListener listener) {
    this.schedule = schedule;
    this.limits = limits;
    this.workers = workers;
    this.executor = executor;
    this.listener = listener;
  }

  /**
   * Waits until a task may run and hands it to a worker, or tells the listener that everything
   * requested is done.
   *
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  // whatever keeps the workers from taking a task, its entity has to be freed
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public void dispatchNext() throws InterruptedException {
    Optional<PublishTask> next = this.schedule.awaitNext(this::capacity);
    if (next.isEmpty()) {
      this.listener.drained();
      return;
    }
    PublishTask task = next.get();
    try {
      this.workers.submit(() -> this.execute(task));
    } catch (RuntimeException e) {
      // a task the workers refuse must not keep its entity taken
      this.schedule.completed(task, PublishOutcome.FAILED);
      this.listener.failed(task, e);
    }
  }

  private PublishingCapacity capacity() {
    return new PublishingCapacity(this.limits.parallel(), this.limits.parallelWhileInteractive());
  }

  // whatever breaks a task, its entity has to be freed and the worker has to survive
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private void execute(PublishTask task) {
    this.schedule.adopt(task.key());
    PublishOutcome outcome = PublishOutcome.FAILED;
    try {
      outcome = this.executor.execute(task);
    } catch (RuntimeException e) {
      this.listener.failed(task, e);
    } finally {
      this.schedule.completed(task, outcome);
    }
  }
}
