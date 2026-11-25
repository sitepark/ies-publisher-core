package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedFileEntry;
import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedObjectFiles;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileType;
import com.sitepark.ies.publisher.core.publishing.port.PublishedObjectFilesCache;
import com.sitepark.ies.publisher.core.publishing.port.PublishedObjectFilesRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
class StorePublishedObjectFilesTest {

  private static final PublishedFileEntry PAGE =
      PublishedFileEntry.builder()
          .key(PublishedFileKey.ofEntity("55", "4711"))
          .type(PublishedFileType.OBJECT)
          .url("/index.php")
          .file("/www/index.php")
          .build();

  private PublishedObjectFilesRepository repository;

  private PublishedObjectFilesCache cache;

  private StorePublishedObjectFiles useCase;

  @BeforeEach
  void setUp() {
    this.repository = mock();
    this.cache = mock();
    this.useCase = new StorePublishedObjectFiles(this.repository, this.cache);
  }

  @Test
  void writesWhatChangedAndKeepsTheResult() {
    PublishedObjectFiles stored = new PublishedObjectFiles("55", "4711", List.of());

    PublishedObjectFiles next = this.useCase.store(stored, List.of(PAGE));

    InOrder order = inOrder(this.repository, this.cache);
    order.verify(this.repository).store(stored.changesTo(next));
    order.verify(this.cache).forgetAt("55", PAGE.path());
    order.verify(this.cache).put(next);
    assertThat(next.files()).as("files afterwards").containsExactly(PAGE);
  }

  @Test
  void writesNothingWhenNothingChanged() {
    PublishedObjectFiles stored = new PublishedObjectFiles("55", "4711", List.of(PAGE));

    PublishedObjectFiles next = this.useCase.store(stored, List.of(PAGE));

    verify(this.repository, never()).store(any());
    verify(this.cache, never()).forgetAt(any(), any());
    verify(this.cache).put(next);
  }
}
