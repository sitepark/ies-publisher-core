package com.sitepark.ies.publisher.core.publishing.usecase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFilePath;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishedFileType;
import com.sitepark.ies.publisher.core.publishing.port.PublishedObjectFilesCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ForgetPublishedObjectFilesTest {

  private PublishedObjectFilesCache cache;

  private ForgetPublishedObjectFiles useCase;

  @BeforeEach
  void setUp() {
    this.cache = mock();
    this.useCase = new ForgetPublishedObjectFiles(this.cache);
  }

  @Test
  void forgetsAnEntity() {
    this.useCase.entity("55", "4711");
    verify(this.cache).forget("55", "4711");
  }

  @Test
  void forgetsTheEntitiesAtAPlace() {
    PublishedFilePath path = new PublishedFilePath(PublishedFileType.OBJECT, "/www/index.php");
    this.useCase.path("55", path);
    verify(this.cache).forgetAt("55", path);
  }

  @Test
  void forgetsAChannel() {
    this.useCase.channel("55");
    verify(this.cache).forgetChannel("55");
  }
}
