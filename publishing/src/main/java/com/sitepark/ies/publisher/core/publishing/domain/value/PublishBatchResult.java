package com.sitepark.ies.publisher.core.publishing.domain.value;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishTask;
import java.util.List;

/**
 * What became of tasks that were requested together and waited for.
 *
 * @param done the number of tasks that were carried out
 * @param failed the tasks that failed for good; must not be {@code null}
 * @param cancelled the number of tasks dropped before they ran, because the queue was cleared
 */
public record PublishBatchResult(int done, List<PublishTask> failed, int cancelled) {

  /**
   * Validates the failed tasks and keeps them unmodifiable.
   *
   * @throws IllegalArgumentException if {@code failed} is {@code null}
   */
  public PublishBatchResult {
    if (failed == null) {
      throw new IllegalArgumentException("Failed tasks must not be null");
    }
    failed = List.copyOf(failed);
  }

  /**
   * Returns whether every task was carried out.
   *
   * @return {@code true} if none failed and none was dropped
   */
  public boolean isComplete() {
    return this.failed.isEmpty() && this.cancelled == 0;
  }
}
