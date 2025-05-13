package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

import java.io.Serial;

public class LinkCheckerException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public LinkCheckerException() {
    super();
  }

  public LinkCheckerException(String message) {
    super(message);
  }

  public LinkCheckerException(String message, Throwable t) {
    super(message, t);
  }
}
