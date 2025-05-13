package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

import java.io.Serial;

public abstract class PublisherException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public PublisherException() {
    super();
  }

  public PublisherException(String message) {
    super(message);
  }

  public PublisherException(String message, Throwable t) {
    super(message, t);
  }
}
