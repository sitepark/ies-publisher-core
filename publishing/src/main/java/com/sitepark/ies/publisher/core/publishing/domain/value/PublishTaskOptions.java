package com.sitepark.ies.publisher.core.publishing.domain.value;

/**
 * How a publish task is to be carried out, as far as it matters when two requests for the same
 * entity are merged into one task.
 *
 * @param triggerDependencies whether entities depending on what the task changes are published in
 *     turn
 * @param forceScaleImages whether scaled images are written again even if they exist
 */
public record PublishTaskOptions(boolean triggerDependencies, boolean forceScaleImages) {

  /** What applies unless a request says otherwise: dependencies are triggered, images kept. */
  public static final PublishTaskOptions DEFAULT = new PublishTaskOptions(true, false);

  /**
   * Returns the options of a task that serves this request and another one: whatever either asks
   * for is done.
   *
   * @param other the options of the other request
   * @return the options covering both
   */
  public PublishTaskOptions merge(PublishTaskOptions other) {
    return new PublishTaskOptions(
        this.triggerDependencies || other.triggerDependencies,
        this.forceScaleImages || other.forceScaleImages);
  }
}
