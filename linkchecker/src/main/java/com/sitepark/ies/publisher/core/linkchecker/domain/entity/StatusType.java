package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

/** Classification of the outcome of checking an external link. */
public enum StatusType {
  /** The link is reachable and responded successfully. */
  OK,
  /** The link requires authentication (HTTP 401). */
  UNAUTHORIZED,
  /** Access to the link is forbidden (HTTP 403). */
  FORBIDDEN,
  /** The link target does not exist (HTTP 404). */
  NOT_FOUND,
  /** The link target reported a server-side error (HTTP 5xx). */
  INTERNAL_SERVER_ERROR,
  /** The request to the link timed out. */
  TIMEOUT,
  /** The link failed for a reason not covered by the other status types. */
  OTHER,
  /** The link was excluded from checking and therefore not evaluated. */
  IGNORED,
  /** The link status could not be determined. */
  UNKNOWN
}
