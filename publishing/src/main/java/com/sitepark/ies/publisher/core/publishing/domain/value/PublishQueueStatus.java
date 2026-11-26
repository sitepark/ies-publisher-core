package com.sitepark.ies.publisher.core.publishing.domain.value;

/**
 * Where the publishing of a server stands.
 *
 * @param waiting the tasks waiting to be handed out
 * @param running the tasks being executed in the background
 * @param foreground the publications running in the foreground, for a user waiting for them
 * @param processed the background tasks finished since the server started, whatever their outcome
 * @param failed the background tasks that failed for good since the server started
 * @param suspended whether publishing is suspended
 */
public record PublishQueueStatus(
    int waiting, int running, int foreground, long processed, long failed, boolean suspended) {

  /**
   * Returns whether nothing is waiting and nothing is being published.
   *
   * @return {@code true} if there is nothing to do
   */
  public boolean isIdle() {
    return this.waiting == 0 && this.running == 0 && this.foreground == 0;
  }
}
