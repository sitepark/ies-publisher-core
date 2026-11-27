package com.sitepark.ies.publisher.core.publishing.domain.service;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;

/** The place a waiting task takes: one per entity, channel and kind of work. */
record SlotKey(PublishTaskKey key, PublishTaskType.Slot slot) {

  static SlotKey of(PublishTask task) {
    return new SlotKey(task.key(), task.type().slot());
  }
}
