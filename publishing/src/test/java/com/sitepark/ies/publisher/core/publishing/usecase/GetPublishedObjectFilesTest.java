package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedObjectFiles;
import com.sitepark.ies.publisher.core.publishing.port.PublishedObjectFilesCache;
import com.sitepark.ies.publisher.core.publishing.port.PublishedObjectFilesRepository;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class GetPublishedObjectFilesTest {

  private static final PublishedObjectFiles FILES =
      new PublishedObjectFiles("55", "4711", List.of());

  private PublishedObjectFilesRepository repository;

  private PublishedObjectFilesCache cache;

  private GetPublishedObjectFiles useCase;

  @BeforeEach
  void setUp() {
    this.repository = mock();
    this.cache = mock();
    this.useCase = new GetPublishedObjectFiles(this.repository, this.cache);
  }

  @Test
  void answersKeptFilesWithoutTheRepository() {
    when(this.cache.getOrLoad(eq("55"), eq("4711"), any())).thenReturn(FILES);

    assertThat(this.useCase.get("55", "4711")).as("the kept files are answered").isSameAs(FILES);
    verify(this.repository, never()).find(anyString(), anyString());
  }

  @Test
  void readsFilesNotKeptFromTheRepository() {
    when(this.repository.find("55", "4711")).thenReturn(FILES);
    when(this.cache.getOrLoad(eq("55"), eq("4711"), any()))
        .thenAnswer(invocation -> invocation.<Supplier<PublishedObjectFiles>>getArgument(2).get());

    assertThat(this.useCase.get("55", "4711"))
        .as("files the cache does not keep are read from the repository")
        .isSameAs(FILES);
  }
}
