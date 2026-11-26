package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.service.PublishSchedule;
import jakarta.inject.Inject;

/**
 * Cancels lists of publish tasks: one that was requested by mistake, or all of them when publishing
 * is stopped.
 *
 * <p>Cancelling drops the tasks still waiting. A task another list asked for as well stays for that
 * list. What is being published is finished.
 */
public class CancelPublishLists {

  private final PublishSchedule schedule;

  /**
   * Creates the use case.
   *
   * @param schedule what is published when
   */
  @Inject
  public CancelPublishLists(PublishSchedule schedule) {
    this.schedule = schedule;
  }

  /**
   * Cancels one list.
   *
   * @param id the list
   * @return {@code false} if no such list has tasks waiting or running
   */
  public boolean cancel(PublishListId id) {
    return this.schedule.cancel(id);
  }

  /**
   * Cancels every list.
   *
   * @return the number of lists cancelled
   */
  public int cancelAll() {
    return this.schedule.cancelAll();
  }
}
