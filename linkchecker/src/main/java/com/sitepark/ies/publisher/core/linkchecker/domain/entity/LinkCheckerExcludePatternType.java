package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

/** Matching strategy used by a {@link LinkCheckerExcludePattern} to match link URLs. */
public enum LinkCheckerExcludePatternType {
  /** The pattern is a regular expression matched against the URL. */
  REGEX,
  /** The URL is excluded if it contains the pattern as a substring. */
  CONTAINS,
  /** The pattern is a glob expression matched against the URL. */
  GLOB,
  /** The URL is excluded only if it equals the pattern exactly. */
  EXACT
}
