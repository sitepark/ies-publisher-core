package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class InvalidLinkCheckerConfigExceptionTest {

  @Test
  void testMessage() {
    InvalidLinkCheckerConfigException e = new InvalidLinkCheckerConfigException("invalid");
    assertEquals("invalid", e.getMessage(), "Unexpected message");
  }

  @Test
  void testMessageAndCause() {
    RuntimeException cause = new RuntimeException("cause");
    InvalidLinkCheckerConfigException e = new InvalidLinkCheckerConfigException("invalid", cause);
    assertSame(cause, e.getCause(), "Unexpected cause");
  }
}
