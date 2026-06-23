package com.sitepark.ies.publisher.core.linkchecker.port;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerBackgroundExecution;

/** Contract for running a link check as an asynchronous background execution. */
public interface LinkCheckerBackgroundExecutor {
  /**
   * Starts the given execution in the background.
   *
   * @param execution the background execution to run
   * @return the identifier of the started background execution
   */
  String execute(LinkCheckerBackgroundExecution execution);
}
