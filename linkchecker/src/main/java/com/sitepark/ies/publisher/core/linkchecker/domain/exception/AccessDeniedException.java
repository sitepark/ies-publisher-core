package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

import java.io.Serial;

public class AccessDeniedException extends PublisherException {
  @Serial private static final long serialVersionUID = 1L;

  public AccessDeniedException(String message) {
    super(message);
  }

  public AccessDeniedException(String message, Throwable t) {
    super(message, t);
  }
}
