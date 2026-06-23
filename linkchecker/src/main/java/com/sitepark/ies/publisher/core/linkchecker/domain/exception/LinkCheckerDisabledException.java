package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

import com.sitepark.ies.sharedkernel.domain.DomainException;
import java.io.Serial;

/** Thrown when a link checker operation is requested while the link checker is disabled. */
public class LinkCheckerDisabledException extends DomainException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates a new exception without a detail message. */
  public LinkCheckerDisabledException() {
    super();
  }

  /**
   * Creates a new exception with the given detail message.
   *
   * @param message the detail message
   */
  public LinkCheckerDisabledException(String message) {
    super(message);
  }

  /**
   * Creates a new exception with the given detail message and cause.
   *
   * @param message the detail message
   * @param t the underlying cause
   */
  public LinkCheckerDisabledException(String message, Throwable t) {
    super(message, t);
  }
}
