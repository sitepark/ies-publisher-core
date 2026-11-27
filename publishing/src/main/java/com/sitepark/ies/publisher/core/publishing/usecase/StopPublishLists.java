package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.exception.AccessDeniedException;
import com.sitepark.ies.publisher.core.publishing.domain.service.PublishSchedule;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import com.sitepark.ies.publisher.core.publishing.port.AccessControl;
import jakarta.inject.Inject;

/**
 * Stops lists on behalf of an administrator: the tasks still waiting are dropped, what is being
 * published is finished. See {@link CancelPublishLists}.
 */
public class StopPublishLists {

  private final AccessControl accessControl;

  private final PublishSchedule schedule;

  /**
   * Creates the use case.
   *
   * @param accessControl checks whether the current user may manage publishing
   * @param schedule what is published when
   */
  @Inject
  public StopPublishLists(AccessControl accessControl, PublishSchedule schedule) {
    this.accessControl = accessControl;
    this.schedule = schedule;
  }

  /**
   * Stops one list.
   *
   * @param id the list
   * @return {@code false} if no such list has tasks waiting or running
   * @throws AccessDeniedException if the current user may not manage publishing
   */
  public boolean stop(PublishListId id) {
    this.checkAccess();
    return this.schedule.cancel(id);
  }

  /**
   * Stops every list.
   *
   * @return the number of lists stopped
   * @throws AccessDeniedException if the current user may not manage publishing
   */
  public int stopAll() {
    this.checkAccess();
    return this.schedule.cancelAll();
  }

  private void checkAccess() {
    if (!this.accessControl.isAllowManagePublishing()) {
      throw new AccessDeniedException("Not allowed to stop publish lists");
    }
  }
}
