package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

public class AccessDeniedException extends PublisherException {
  private static final long serialVersionUID = 1L;

  public AccessDeniedException(String message) {
    super(message);
  }

  public AccessDeniedException(String message, Throwable t) {
    super(message, t);
  }
}
