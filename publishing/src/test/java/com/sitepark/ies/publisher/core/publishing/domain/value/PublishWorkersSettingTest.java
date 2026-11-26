package com.sitepark.ies.publisher.core.publishing.domain.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PublishWorkersSettingTest {

  @Test
  void auto_takesNoNumber() {
    assertThat(PublishWorkersSetting.auto().workers())
        .as("an automatic setting should carry no number of workers")
        .isNull();
  }

  @Test
  void fixed_carriesTheNumber() {
    assertThat(PublishWorkersSetting.fixed(4).workers())
        .as("a fixed setting should carry the number given")
        .isEqualTo(4);
  }

  @Test
  void fixedWithoutWorkers_throwsException() {
    assertThatThrownBy(() -> PublishWorkersSetting.fixed(0))
        .as("a fixed setting below one worker should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void autoWithWorkers_throwsException() {
    assertThatThrownBy(() -> new PublishWorkersSetting(PublishWorkersSetting.Mode.AUTO, 4))
        .as("an automatic setting with a number should throw an IllegalArgumentException")
        .isInstanceOf(IllegalArgumentException.class);
  }
}
