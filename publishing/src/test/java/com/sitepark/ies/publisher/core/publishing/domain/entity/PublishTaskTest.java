package com.sitepark.ies.publisher.core.publishing.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jparams.verifier.tostring.ToStringVerifier;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishPriority;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskOptions;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishTaskType;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.Test;

class PublishTaskTest {

  private static final PublishTaskKey KEY = new PublishTaskKey("55", "4711");

  @Test
  void testEquals() {
    EqualsVerifier.forClass(PublishTask.class).suppress(Warning.NULL_FIELDS).verify();
  }

  @Test
  void testToString() {
    ToStringVerifier.forClass(PublishTask.class).verify();
  }

  @Test
  void of_createsANewTaskWithDefaults() {
    assertThat(PublishTask.of(KEY, PublishTaskType.PUBLISH, PublishPriority.BULK))
        .as("a new task waits for nothing, has default options and no failed attempt")
        .isEqualTo(
            new PublishTask(
                KEY,
                PublishTaskType.PUBLISH,
                PublishPriority.BULK,
                false,
                PublishTaskOptions.DEFAULT,
                0,
                null));
  }

  @Test
  void nextAttempt_countsTheAttempt() {
    PublishTask task = PublishTask.of(KEY, PublishTaskType.PUBLISH, PublishPriority.BULK);

    assertThat(task.nextAttempt().attempt())
        .as("the failed attempt should be counted")
        .isEqualTo(1);
  }

  @Test
  void nullKey_throwsException() {
    assertThatThrownBy(() -> PublishTask.of(null, PublishTaskType.PUBLISH, PublishPriority.BULK))
        .as("null key should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cleanupAsFollowUp_throwsException() {
    PublishTask cleanup = PublishTask.of(KEY, PublishTaskType.CLEANUP, PublishPriority.BULK);

    assertThatThrownBy(cleanup::asFollowUp)
        .as("a cleanup as follow-up should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }
}
