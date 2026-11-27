package com.sitepark.ies.publisher.core.publishing.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jparams.verifier.tostring.ToStringVerifier;
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
    assertThat(PublishTask.of(KEY, PublishTaskType.PUBLISH))
        .as("a new task has default options and no failed attempt")
        .isEqualTo(
            new PublishTask(KEY, PublishTaskType.PUBLISH, PublishTaskOptions.DEFAULT, 0, null));
  }

  @Test
  void nextAttempt_countsTheAttempt() {
    PublishTask task = PublishTask.of(KEY, PublishTaskType.PUBLISH);

    assertThat(task.nextAttempt().attempt())
        .as("the failed attempt should be counted")
        .isEqualTo(1);
  }

  @Test
  void nullKey_throwsException() {
    assertThatThrownBy(() -> PublishTask.of(null, PublishTaskType.PUBLISH))
        .as("null key should throw a NullPointerException")
        .isInstanceOf(NullPointerException.class);
  }
}
