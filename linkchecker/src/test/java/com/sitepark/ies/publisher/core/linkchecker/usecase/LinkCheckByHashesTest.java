package com.sitepark.ies.publisher.core.linkchecker.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.DailyScheduling;
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

  private static final String BROKEN_HASH = "h1";

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
            .scheduling(DailyScheduling.builder().startTime(3, 0).build())
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

  @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
  @Test
  void testFailingCheckDoesNotLoseOtherResults() {

    AccessControl accessControl = mock();
    when(accessControl.isAllowRunLinkChecker()).thenReturn(true);
    PublishedExternalLinkRepository publishedExternalLinkRepository = mock();
    LinkCheckerConfigStore linkCheckerConfigStore = mock();
    when(linkCheckerConfigStore.get())
        .thenReturn(
            LinkCheckerConfig.builder()
                .enabled(true)
                .parallel(2)
                .scheduling(DailyScheduling.builder().startTime(3, 0).build())
                .build());

    LinkCheckerLink broken =
        LinkCheckerLink.builder().hash(BROKEN_HASH).url("https://broken").build();
    LinkCheckerLink fine = LinkCheckerLink.builder().hash("h2").url("https://fine").build();
    when(publishedExternalLinkRepository.getLinks(List.of("h1", "h2")))
        .thenReturn(List.of(broken, fine));

    LinkChecker linkChecker = mock();
    when(linkChecker.checkLink(any()))
        .thenAnswer(
            invocation -> {
              LinkCheckerLink link = invocation.getArgument(0);
              if (BROKEN_HASH.equals(link.getHash())) {
                throw new IllegalStateException("boom");
              }
              return LinkCheckerResultItem.builder().status(StatusType.OK).build();
            });

    LinkCheckByHashes useCase =
        new LinkCheckByHashes(
            accessControl, linkChecker, publishedExternalLinkRepository, linkCheckerConfigStore);

    List<LinkCheckerResultItem> result = useCase.linkCheckByHashes(List.of("h1", "h2"));

    assertEquals(
        List.of(
            LinkCheckerResultItem.builder()
                .url("https://broken")
                .hash("h1")
                .status(StatusType.OTHER)
                .message("boom")
                .build(),
            LinkCheckerResultItem.builder()
                .url("https://fine")
                .hash("h2")
                .status(StatusType.OK)
                .build()),
        result,
        "A failing link must be reported without losing the others, in link order");
    verify(publishedExternalLinkRepository, never()).updateCheckResult(eq("h1"), any());
  }
}
