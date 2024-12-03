package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

public class LinkCheckerException extends RuntimeException {

  private static final long serialVersionUID = 1L;

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
