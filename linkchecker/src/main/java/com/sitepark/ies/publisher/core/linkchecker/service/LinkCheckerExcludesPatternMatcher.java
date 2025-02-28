package com.sitepark.ies.publisher.core.linkchecker.service;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerExcludePattern;
import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerExcludePatternType;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

public class LinkCheckerExcludesPatternMatcher {

  private final List<LinkCheckerExcludePattern> patternList;

  public LinkCheckerExcludesPatternMatcher(List<LinkCheckerExcludePattern> patternList) {
    this.patternList = new ArrayList<>(patternList);
  }

  public boolean isExcluded(String url) {
    for (LinkCheckerExcludePattern pattern : this.patternList) {
      if (this.matches(url, pattern)) {
        return true;
      }
    }
    return false;
  }

  private boolean matches(String url, LinkCheckerExcludePattern pattern) {
    return switch (pattern.type()) {
      case LinkCheckerExcludePatternType.REGEX -> this.matchRegex(url, pattern.pattern());
      case LinkCheckerExcludePatternType.CONTAINS -> this.matchContains(url, pattern.pattern());
      case LinkCheckerExcludePatternType.GLOB -> this.matchGlob(url, pattern.pattern());
      case LinkCheckerExcludePatternType.EXACT -> this.matchExact(url, pattern.pattern());
    };
  }

  private boolean matchRegex(String url, String pattern) {
    return url.matches(pattern);
  }

  private boolean matchContains(String url, String pattern) {
    return url.contains(pattern);
  }

  private boolean matchGlob(String url, String pattern) {
    PathMatcher pathMatcher =
        java.nio.file.FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    return pathMatcher.matches(java.nio.file.Paths.get(url));
  }

  private boolean matchExact(String url, String pattern) {
    return url.equals(pattern);
  }
}
