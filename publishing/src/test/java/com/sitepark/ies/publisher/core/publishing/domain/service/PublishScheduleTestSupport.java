package com.sitepark.ies.publisher.core.publishing.domain.service;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList;
import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListOrigin;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListRequest;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishingCapacity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;

/** The setup and helpers the tests of {@link PublishSchedule} share. */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
// abstract: only the subclasses hold tests; it is no test class of its own
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
abstract class PublishScheduleTestSupport {

  static final PublishingCapacity FOUR = new PublishingCapacity(4, 1);

  static final PublishingCapacity ONE = new PublishingCapacity(1, 1);

  PublishSchedule schedule;

  @BeforeEach
  void setUp() {
    this.schedule = new PublishSchedule();
  }

  String next(PublishingCapacity capacity) {
    return this.schedule.pollNext(capacity).map(task -> task.key().entityId()).orElse("nothing");
  }

  PublishListId urgent(String... entities) {
    return this.schedule.open(request(PublishPriority.URGENT, entities));
  }

  PublishListId normal(String... entities) {
    return this.schedule.open(request(PublishPriority.NORMAL, entities));
  }

  PublishListId bulk(String... entities) {
    return this.schedule.open(request(PublishPriority.BULK, entities));
  }

  /** An urgent list that comes after another one. */
  PublishListId after(PublishListId predecessor, String... entities) {
    return this.schedule.open(request(PublishPriority.URGENT, entities).after(Set.of(predecessor)));
  }

  PublishListId open(PublishPriority priority, PublishTask... tasks) {
    return this.schedule.open(
        PublishListRequest.of("test", PublishListOrigin.ADMINISTRATION, priority, List.of(tasks)));
  }

  CompletableFuture<PublishList> awaitList(PublishListId id) {
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

  static PublishListRequest request(PublishPriority priority, String... entities) {
    List<PublishTask> tasks = new ArrayList<>();
    for (String entity : entities) {
      tasks.add(publish(entity));
    }
    return PublishListRequest.of("test", PublishListOrigin.ADMINISTRATION, priority, tasks);
  }

  static PublishTaskKey key(String entity) {
    return new PublishTaskKey("55", entity);
  }

  static PublishTask task(String entity, PublishTaskType type) {
    return PublishTask.of(key(entity), type);
  }

  static PublishTask publish(String entity) {
    return task(entity, PublishTaskType.PUBLISH);
  }

  static PublishTask revoke(String entity) {
    return task(entity, PublishTaskType.DEPUBLISH);
  }

  static PublishTask cleanup(String entity) {
    return task(entity, PublishTaskType.CLEANUP);
  }

  static void await(BooleanSupplier condition) {
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

  /** A clock that stands still until it is moved on. */
  static final class MovableClock extends Clock {

    private Instant now = Instant.parse("2026-01-01T00:00:00Z");

    void moveOn(Duration duration) {
      this.now = this.now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
      return this.now;
    }
  }
}
