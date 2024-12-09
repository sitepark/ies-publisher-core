package com.sitepark.ies.publisher.core.linkchecker.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLinkFilter;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import org.junit.jupiter.api.Test;

class GetCheckResultTest {

  @Test
  void testAccessDenied() {
    AccessControl accessControl = mock();
    when(accessControl.isAllowGetCheckResults()).thenReturn(false);
    GetCheckResult useCase = new GetCheckResult(accessControl, null);
    assertThrows(
        AccessDeniedException.class,
        () -> {
          useCase.getCheckResult(null, 0, 0);
        });
  }

  @Test
  void testGetCheckResult() {
    AccessControl accessControl = mock();
    PublishedExternalLinkRepository publishedExternalLinkRepository = mock();
    LinkCheckerLinkFilter filter = mock();

    when(accessControl.isAllowGetCheckResults()).thenReturn(true);
    GetCheckResult useCase = new GetCheckResult(accessControl, publishedExternalLinkRepository);
    useCase.getCheckResult(filter, 1, 2);

    verify(publishedExternalLinkRepository).getCheckResult(filter, 1, 2);
  }
}
