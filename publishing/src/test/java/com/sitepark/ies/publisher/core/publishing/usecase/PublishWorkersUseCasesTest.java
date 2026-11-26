package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.publishing.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishWorkersSetting;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishingResources;
import com.sitepark.ies.publisher.core.publishing.port.AccessControl;
import com.sitepark.ies.publisher.core.publishing.port.PublishWorkersSettingStore;
import com.sitepark.ies.publisher.core.publishing.port.PublishingResourcesProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class PublishWorkersUseCasesTest {

  private AccessControl accessControl;

  private PublishWorkersSettingStore store;

  private GetPublishWorkers get;

  private SetPublishWorkers set;

  @BeforeEach
  void setUp() {
    this.accessControl = mock();
    this.store = mock();
    PublishingResourcesProvider resources = mock();
    when(resources.get()).thenReturn(new PublishingResources(16, 40));
    this.get = new GetPublishWorkers(this.accessControl, this.store, resources);
    this.set = new SetPublishWorkers(this.accessControl, this.store, resources);
  }

  @Test
  void tellsTheWorkersOfTheStoredSetting() {
    when(this.accessControl.isAllowManagePublishing()).thenReturn(true);
    when(this.store.get()).thenReturn(PublishWorkersSetting.auto());

    assertThat(this.get.get().effective())
        .as("left to the server, half of its 16 processors publish")
        .isEqualTo(8);
  }

  @Test
  void refusesToTellWhoMayNotManagePublishing() {
    when(this.accessControl.isAllowManagePublishing()).thenReturn(false);

    assertThatThrownBy(() -> this.get.get())
        .as("the workers are no business of a user without the permission")
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void storesTheNewSetting() {
    when(this.accessControl.isAllowManagePublishing()).thenReturn(true);

    this.set.set(PublishWorkersSetting.fixed(4));

    verify(this.store).store(PublishWorkersSetting.fixed(4));
  }

  @Test
  void tellsTheWorkersOfTheNewSetting() {
    when(this.accessControl.isAllowManagePublishing()).thenReturn(true);

    assertThat(this.set.set(PublishWorkersSetting.fixed(4)).effective())
        .as("the administrator sees what the setting leads to")
        .isEqualTo(4);
  }

  @Test
  void refusesToStoreForWhoMayNotManagePublishing() {
    when(this.accessControl.isAllowManagePublishing()).thenReturn(false);

    assertThatThrownBy(() -> this.set.set(PublishWorkersSetting.auto()))
        .as("a user without the permission must not change the workers")
        .isInstanceOf(AccessDeniedException.class);
    verify(this.store, never()).store(any());
  }
}
