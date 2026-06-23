package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

import com.sitepark.ies.sharedkernel.domain.DomainException;
import java.io.Serial;

/** Thrown when the current user is not allowed to perform a link checker operation. */
public class AccessDeniedException extends DomainException {
  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates a new exception with the given detail message.
   *
   * @param message the detail message
   */
  public AccessDeniedException(String message) {
    super(message);
  }

  /**
   * Creates a new exception with the given detail message and cause.
   *
   * @param message the detail message
   * @param t the underlying cause
   */
  public AccessDeniedException(String message, Throwable t) {
    super(message, t);
  }
}
