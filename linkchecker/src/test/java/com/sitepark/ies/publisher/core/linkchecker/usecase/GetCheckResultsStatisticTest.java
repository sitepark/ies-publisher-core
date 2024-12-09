package com.sitepark.ies.publisher.core.linkchecker.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import org.junit.jupiter.api.Test;

class GetCheckResultsStatisticTest {

  @Test
  void testAccessDenied() {
    AccessControl accessControl = mock();
    when(accessControl.isAllowGetCheckResults()).thenReturn(false);
    GetCheckResultsStatistic useCase = new GetCheckResultsStatistic(accessControl, null);
    assertThrows(
        AccessDeniedException.class,
        () -> {
          useCase.getCheckResultsStatistic();
        });
  }

  @Test
  void testGetCheckResultsStatistic() {
    AccessControl accessControl = mock();
    when(accessControl.isAllowGetCheckResults()).thenReturn(true);
    PublishedExternalLinkRepository publishedExternalLinkRepository = mock();

    GetCheckResultsStatistic useCase =
        new GetCheckResultsStatistic(accessControl, publishedExternalLinkRepository);
    useCase.getCheckResultsStatistic();

    verify(publishedExternalLinkRepository).getCheckResultsStatistic();
  }
}
