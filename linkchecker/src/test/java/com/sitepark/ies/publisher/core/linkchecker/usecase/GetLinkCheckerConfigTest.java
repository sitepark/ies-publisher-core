package com.sitepark.ies.publisher.core.linkchecker.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerConfigStore;
import org.junit.jupiter.api.Test;

class GetLinkCheckerConfigTest {

  @Test
  void testAccessDenied() {
    AccessControl accessControl = mock();
    when(accessControl.isAllowGetLinkCheckerConfig()).thenReturn(false);
    GetLinkCheckerConfig useCase = new GetLinkCheckerConfig(accessControl, null);
    assertThrows(
        AccessDeniedException.class,
        () -> {
          useCase.get();
        });
  }

  @Test
  void testGetLinkCheckerConfig() {
    AccessControl accessControl = mock();
    when(accessControl.isAllowGetLinkCheckerConfig()).thenReturn(true);

    LinkCheckerConfigStore linkCheckerConfigStore = mock();

    GetLinkCheckerConfig useCase = new GetLinkCheckerConfig(accessControl, linkCheckerConfigStore);
    useCase.get();

    verify(linkCheckerConfigStore).get();
  }
}
