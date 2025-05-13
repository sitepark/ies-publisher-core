package com.sitepark.ies.publisher.core.linkchecker.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerConfig;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerExcludePattern;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerExcludePatternType;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerLink;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerResultItem;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.StatusType;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.linkchecker.port.AccessControl;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkChecker;
import com.sitepark.ies.publisher.core.linkchecker.port.LinkCheckerConfigStore;
import com.sitepark.ies.publisher.core.linkchecker.port.PublishedExternalLinkRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class LinkCheckByHashesTest {

  @Test
  void testAccessDenied() {
    AccessControl accessControl = mock();
    when(accessControl.isAllowRunLinkChecker()).thenReturn(false);
    LinkCheckByHashes useCase = new LinkCheckByHashes(accessControl, null, null, null);
    assertThrows(
        AccessDeniedException.class, () -> useCase.linkCheckByHashes(Collections.emptyList()));
  }

  @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
  @Test
  void testCheck() {

    AccessControl accessControl = mock();
    when(accessControl.isAllowRunLinkChecker()).thenReturn(true);
    PublishedExternalLinkRepository publishedExternalLinkRepository = mock();
    LinkCheckerConfigStore linkCheckerConfigStore = mock();
    LinkCheckerConfig config =
        LinkCheckerConfig.builder()
            .enabled(true)
            .exclude(new LinkCheckerExcludePattern(LinkCheckerExcludePatternType.CONTAINS, "test"))
            .parallel(1)
            .build();

    when(linkCheckerConfigStore.get()).thenReturn(config);

    List<LinkCheckerLink> links =
        List.of(
            LinkCheckerLink.builder().hash("hash1").url("https://www.test.com").build(),
            LinkCheckerLink.builder().hash("hash2").url("https://www.sitepark.com").build());

    when(publishedExternalLinkRepository.getLinks(List.of("hash1", "hash2"))).thenReturn(links);

    LinkChecker linkChecker = mock();
    when(linkChecker.checkLink(any()))
        .thenReturn(LinkCheckerResultItem.builder().status(StatusType.OK).build());

    LinkCheckByHashes useCase =
        new LinkCheckByHashes(
            accessControl, linkChecker, publishedExternalLinkRepository, linkCheckerConfigStore);

    List<LinkCheckerResultItem> result = useCase.linkCheckByHashes(List.of("hash1", "hash2"));

    verify(publishedExternalLinkRepository, never())
        .updateCheckResult("hash1", LinkCheckerResultItem.builder().status(StatusType.OK).build());
    verify(publishedExternalLinkRepository)
        .updateCheckResult("hash2", LinkCheckerResultItem.builder().status(StatusType.OK).build());

    assertEquals(
        List.of(
            LinkCheckerResultItem.builder()
                .url("https://www.sitepark.com")
                .hash("hash2")
                .status(StatusType.OK)
                .build()),
        result,
        "Unexpected result");
  }
}
