package com.sitepark.ies.publisher.core.linkchecker.domain.entity;

/**
 * Pattern that excludes matching URLs from being checked.
 *
 * @param type the matching strategy applied to the pattern
 * @param pattern the pattern value matched against link URLs
 */
public record LinkCheckerExcludePattern(LinkCheckerExcludePatternType type, String pattern) {}
