package com.sitepark.ies.publisher.core.publishing.domain.value;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PublishedFilePathTest {

  @Test
  void rejectsAMissingType() {
    assertThatThrownBy(() -> new PublishedFilePath(null, "/www/index.php"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsABlankFile() {
    assertThatThrownBy(() -> new PublishedFilePath(PublishedFileType.OBJECT, " "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
