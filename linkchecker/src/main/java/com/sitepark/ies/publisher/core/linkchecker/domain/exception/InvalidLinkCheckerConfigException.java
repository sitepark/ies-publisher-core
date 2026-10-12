package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

import java.io.Serial;

/** Raised when a link checker configuration contains an invalid value. */
public class InvalidLinkCheckerConfigException extends LinkCheckerException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates a new exception with the given detail message.
   *
   * @param message the detail message
   */
  public InvalidLinkCheckerConfigException(String message) {
    super(message);
  }

  /**
   * Creates a new exception with the given detail message and cause.
   *
   * @param message the detail message
   * @param cause the underlying cause
   */
  public InvalidLinkCheckerConfigException(String message, Throwable cause) {
    super(message, cause);
  }
}
