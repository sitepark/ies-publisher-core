package com.sitepark.ies.publisher.core.publishing.domain.service;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/** An entity being published. Only touched under the lock of {@link PublishSchedule}. */
final class Hold {

  /** The depth of a publication that is not nested in another one of the same entity. */
  static final int OUTERMOST = 1;

  final boolean foreground;

  final PublishTask task;

  /** The lists waiting for the publication to end. */
  final Set<ListState> lists = new HashSet<>();

  /** The publishing thread; unknown for a background task until its worker adopts it. */
  @Nullable Thread owner;

  int depth = OUTERMOST;

  private Hold(boolean foreground, PublishTask task, @Nullable Thread owner) {
    this.foreground = foreground;
    this.task = task;
    this.owner = owner;
  }

  static Hold ofBackground(WaitingTask waiting) {
    return new Hold(false, waiting.task, null);
  }

  static Hold ofForeground(Thread owner, PublishTask task) {
    return new Hold(true, task, owner);
  }

  /** The list waits for this publication from now on. */
  void add(@Nullable ListState list, Instant now) {
    if (list != null && this.lists.add(list)) {
      list.taskStarted(now);
    }
  }

  /** The publication stopped; the lists still wait until it is settled or tried again. */
  void stopped() {
    this.lists.forEach(ListState::taskStopped);
  }
}
