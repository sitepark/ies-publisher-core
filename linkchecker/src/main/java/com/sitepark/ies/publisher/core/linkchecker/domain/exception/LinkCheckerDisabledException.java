package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

public class LinkCheckerDisabledException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public LinkCheckerDisabledException() {
    super();
  }

  public LinkCheckerDisabledException(String message) {
    super(message);
  }

  public LinkCheckerDisabledException(String message, Throwable t) {
    super(message, t);
  }
}
