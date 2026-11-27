package com.sitepark.ies.publisher.core.publishing.usecase;

import com.sitepark.ies.publisher.core.publishing.domain.entity.PublishList;
import com.sitepark.ies.publisher.core.publishing.domain.service.PublishSchedule;
import com.sitepark.ies.publisher.core.publishing.domain.value.PublishListId;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

/**
 * Tells what is being published and why: the lists that were requested, each with its name, who
 * asked for it, and how far it has come.
 */
public class GetPublishLists {

  private final PublishSchedule schedule;

  /**
   * Creates the use case.
   *
   * @param schedule what is published when
   */
  @Inject
  public GetPublishLists(PublishSchedule schedule) {
    this.schedule = schedule;
  }

  /**
   * Returns the lists with tasks waiting or running, oldest first, followed by the lists finished
   * most recently, youngest first.
   *
   * @return the lists as they stand
   */
  public List<PublishList> all() {
    return this.schedule.lists();
  }

  /**
   * Returns one list as it stands.
   *
   * @param id the list
   * @return the list, or empty if it is unknown or was finished too long ago
   */
  public Optional<PublishList> get(PublishListId id) {
    return this.schedule.list(id);
  }

  /**
   * Waits until no task of the list is waiting or running any more.
   *
   * @param id the list
   * @return the list as it ended
   * @throws InterruptedException if the thread is interrupted while waiting
   * @throws IllegalArgumentException if no such list is known
   */
  public PublishList await(PublishListId id) throws InterruptedException {
    return this.schedule.await(id);
  }
}
