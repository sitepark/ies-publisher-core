package com.sitepark.ies.publisher.core.publishing.domain.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** How many waiting cleanups each list has, worked out once and only if needed. */
// counts the lists of the waiting cleanups, state of this package
@SuppressWarnings("PMD.LawOfDemeter")
final class CleanupCounts {

  private final Collection<WaitingTask> cleanups;

  private @Nullable Map<ListState, Integer> counts;

  CleanupCounts(Collection<WaitingTask> cleanups) {
    this.cleanups = cleanups;
  }

  int of(ListState list) {
    Map<ListState, Integer> known = this.counts;
    if (known == null) {
      known = new HashMap<>();
      for (WaitingTask cleanup : this.cleanups) {
        for (ListState owner : cleanup.lists) {
          known.merge(owner, 1, Integer::sum);
        }
      }
      this.counts = known;
    }
    return known.getOrDefault(list, 0);
  }
}
