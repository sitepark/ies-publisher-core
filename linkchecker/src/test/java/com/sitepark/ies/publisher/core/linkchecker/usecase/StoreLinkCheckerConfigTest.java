package com.sitepark.ies.publisher.core.linkchecker.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerConfigStore;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerScheduler;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import org.junit.jupiter.api.Test;

class StoreLinkCheckerConfigTest {

  @Test
  void testAccessDenied() {
    AccessControl accessControl = mock();
    when(accessControl.isAllowGetLinkCheckerConfig()).thenReturn(false);
    StoreLinkCheckerConfig useCase = new StoreLinkCheckerConfig(accessControl, null, null, null);
    assertThrows(
        AccessDeniedException.class,
        () -> {
          useCase.store(null);
        });
  }

  @Test
  void testStoreLinkCheckerConfig() {
    AccessControl accessControl = mock();
    when(accessControl.isAllowGetLinkCheckerConfig()).thenReturn(true);
    LinkCheckerConfigStore linkCheckerConfigStore = mock();
    LinkCheckerScheduler linkCheckerScheduler = mock();
    LinkCheckerConfig config = mock();
    when(linkCheckerConfigStore.get()).thenReturn(config);
    when(config.isEnabled()).thenReturn(true);
    PublishedExternalLinkRepository publishedExternalLinkRepository = mock();

    StoreLinkCheckerConfig useCase =
        new StoreLinkCheckerConfig(
            accessControl,
            linkCheckerConfigStore,
            linkCheckerScheduler,
            publishedExternalLinkRepository);
    useCase.store(config);

    verify(linkCheckerConfigStore).store(config);
    verify(linkCheckerScheduler).configChanged();
  }
}
