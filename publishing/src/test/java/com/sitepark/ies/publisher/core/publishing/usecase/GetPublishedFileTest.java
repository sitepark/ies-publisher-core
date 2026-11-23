package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishedFile;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileKey;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileType;
import com.sitepark.ies.publisher.core.publishing.port.PublishedFileRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetPublishedFileTest {

  private static final PublishedFileKey KEY = PublishedFileKey.ofEntity("55", "1118");

  private PublishedFileRepository repository;

  private GetPublishedFile useCase;

  @BeforeEach
  void setUp() {
    this.repository = mock();
    this.useCase = new GetPublishedFile(this.repository);
  }

  @Test
  void returnsThePublishedFile() {
    PublishedFile file =
        PublishedFile.builder()
            .key(KEY)
            .type(PublishedFileType.OBJECT)
            .url("/index.php")
            .file("/index.php")
            .build();
    when(this.repository.find(KEY)).thenReturn(Optional.of(file));

    assertThat(this.useCase.get(KEY))
        .as("the file the repository holds for the key should be returned")
        .contains(file);
  }

  @Test
  void returnsNothingForAnEntityNotPublished() {
    when(this.repository.find(KEY)).thenReturn(Optional.empty());

    assertThat(this.useCase.get(KEY))
        .as("an entity the channel holds no file of should yield nothing")
        .isEmpty();
  }
}
