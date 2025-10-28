package com.sitepark.ies.publisher.core.linkchecker.domain.exception;

import com.sitepark.ies.sharedkernel.domain.DomainException;
import java.io.Serial;

public class LinkCheckerDisabledException extends DomainException {

  @Serial private static final long serialVersionUID = 1L;

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
