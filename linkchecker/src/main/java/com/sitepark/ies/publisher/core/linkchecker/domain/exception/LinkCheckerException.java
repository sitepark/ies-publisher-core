package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

import com.sitepark.ies.sharedkernel.domain.DomainException;
import java.io.Serial;

/** Base exception for errors raised by the link checker domain. */
public class LinkCheckerException extends DomainException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates a new exception without a detail message. */
  public LinkCheckerException() {
    super();
  }

  /**
   * Creates a new exception with the given detail message.
   *
   * @param message the detail message
   */
  public LinkCheckerException(String message) {
    super(message);
  }

  /**
   * Creates a new exception with the given detail message and cause.
   *
   * @param message the detail message
   * @param t the underlying cause
   */
  public LinkCheckerException(String message, Throwable t) {
    super(message, t);
  }
}
