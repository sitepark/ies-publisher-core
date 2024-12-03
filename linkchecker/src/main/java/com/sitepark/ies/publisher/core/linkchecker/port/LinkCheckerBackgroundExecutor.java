package com.sitepark.ies.publisher.core.linkchecker.port;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerBackgroundExecution;

public interface LinkCheckerBackgroundExecutor {
  String execute(LinkCheckerBackgroundExecution execution);
}
