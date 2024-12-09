package com.sitepark.ies.publisher.core.linkchecker.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerExcludePattern;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerExcludePatternType;
import java.util.List;
import org.junit.jupiter.api.Test;

class LinkCheckerExcludesPatternMatcherTest {

  @Test
  void testRegexPatternIsExcluded() {
    LinkCheckerExcludesPatternMatcher matcher =
        this.creatMatcher(LinkCheckerExcludePatternType.REGEX, ".*\\.pdf");
    assertTrue(matcher.isExcluded("http://example.com/test.pdf"));
  }

  @Test
  void testRegexPatternIsNotExcluded() {
    LinkCheckerExcludesPatternMatcher matcher =
        this.creatMatcher(LinkCheckerExcludePatternType.REGEX, ".*\\.pdf");
    assertFalse(matcher.isExcluded("http://example.com/test.html"));
  }

  @Test
  void testContainsPatternIsExcluded() {
    LinkCheckerExcludesPatternMatcher matcher =
        this.creatMatcher(LinkCheckerExcludePatternType.CONTAINS, "example.com");
    assertTrue(matcher.isExcluded("http://example.com/test.pdf"));
  }

  @Test
  void testContainsPatternIsNotExcluded() {
    LinkCheckerExcludesPatternMatcher matcher =
        this.creatMatcher(LinkCheckerExcludePatternType.CONTAINS, "example.com");
    assertFalse(matcher.isExcluded("http://example.de/test.html"));
  }

  @Test
  void testGlobPatternIsExcluded() {
    LinkCheckerExcludesPatternMatcher matcher =
        this.creatMatcher(LinkCheckerExcludePatternType.GLOB, "**.pdf");
    assertTrue(matcher.isExcluded("http://example.com/path/test.pdf"));
  }

  @Test
  void testGlobPatternIsNotExcluded() {
    LinkCheckerExcludesPatternMatcher matcher =
        this.creatMatcher(LinkCheckerExcludePatternType.GLOB, "**.pdf");
    assertFalse(matcher.isExcluded("http://example.de/path/test.html"));
  }

  @Test
  void testExactPatternIsExcluded() {
    LinkCheckerExcludesPatternMatcher matcher =
        this.creatMatcher(LinkCheckerExcludePatternType.EXACT, "http://example.com/path/test.pdf");
    assertTrue(matcher.isExcluded("http://example.com/path/test.pdf"));
  }

  @Test
  void testExactPatternIsNotExcluded() {
    LinkCheckerExcludesPatternMatcher matcher =
        this.creatMatcher(LinkCheckerExcludePatternType.EXACT, "http://example.com/path/test.pdf");
    assertFalse(matcher.isExcluded("http://example.com/path/test.html"));
  }

  private LinkCheckerExcludesPatternMatcher creatMatcher(
      LinkCheckerExcludePatternType type, String pattern) {
    LinkCheckerExcludePattern regex = new LinkCheckerExcludePattern(type, pattern);
    return new LinkCheckerExcludesPatternMatcher(List.of(regex));
  }
}
