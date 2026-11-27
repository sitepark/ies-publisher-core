package com.sitepark.ies.publisher.core.linkchecker.service;

import com.sitepark.ies.publisher.core.linkchecker.domain.entity.LinkCheckerExcludePattern;
import com.sitepark.ies.publisher.core.linkchecker.domain.exception.InvalidLinkCheckerConfigException;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Domain service that decides whether a URL is excluded from link checking by matching it against a
 * configured list of exclude patterns (regex, contains, glob or exact).
 *
 * <p>The patterns are compiled once when the matcher is created, so an invalid pattern is reported
 * right away and not for every link that is checked.
 */
public class LinkCheckerExcludesPatternMatcher {

  private final List<Predicate<String>> matchers;

  /**
   * Creates a matcher for the given list of exclude patterns.
   *
   * @param patternList the exclude patterns to match URLs against
   * @throws InvalidLinkCheckerConfigException if a regex or glob pattern is invalid
   */
  public LinkCheckerExcludesPatternMatcher(List<LinkCheckerExcludePattern> patternList) {
    this.matchers = patternList.stream().map(LinkCheckerExcludesPatternMatcher::compile).toList();
  }

  /**
   * Returns whether the given URL matches any of the configured exclude patterns.
   *
   * @param url the URL to test
   * @return {@code true} if the URL is excluded, {@code false} otherwise
   */
  public boolean isExcluded(String url) {
    for (Predicate<String> matcher : this.matchers) {
      if (matcher.test(url)) {
        return true;
      }
    }
    return false;
  }

  private static Predicate<String> compile(LinkCheckerExcludePattern pattern) {
    String value = pattern.pattern();
    return switch (pattern.type()) {
      case REGEX -> compileRegex(value, value);
      case CONTAINS -> url -> url.contains(value);
      case GLOB -> compileRegex(globToRegex(value), value);
      case EXACT -> url -> url.equals(value);
    };
  }

  private static Predicate<String> compileRegex(String regex, String original) {
    try {
      return Pattern.compile(regex).asMatchPredicate();
    } catch (PatternSyntaxException e) {
      throw new InvalidLinkCheckerConfigException("Invalid exclude pattern '" + original + "'", e);
    }
  }

  /**
   * Translates a glob into a regular expression. Works on the URL as a plain string: a URL is no
   * file path, and a path would turn {@code https://host} into {@code https:/host}.
   *
   * <p>{@code **} matches across {@code /}, {@code *} and {@code ?} do not. {@code [...]} and
   * {@code {a,b}} work as in {@link java.nio.file.FileSystem#getPathMatcher(String)}.
   */
  private static String globToRegex(String glob) {
    return new GlobTranslator(glob).translate();
  }

  /** Walks a glob character by character and builds the regular expression for it. */
  private static final class GlobTranslator {

    private final String glob;

    private boolean inGroup;

    private int pos;

    private GlobTranslator(String glob) {
      this.glob = glob;
    }

    private String translate() {
      StringBuilder regex = new StringBuilder();
      while (this.pos < this.glob.length()) {
        this.translateNext(regex);
        this.pos++;
      }
      if (this.inGroup) {
        throw this.invalid("missing '}'");
      }
      return regex.toString();
    }

    private void translateNext(StringBuilder regex) {
      char c = this.glob.charAt(this.pos);
      switch (c) {
        case '*' -> this.star(regex);
        case '?' -> regex.append("[^/]");
        case '\\' -> this.escaped(regex);
        case '[' -> this.characterClass(regex);
        case '{' -> this.groupStart(regex);
        case '}' -> this.groupEnd(regex);
        case ',' -> regex.append(this.inGroup ? "|" : Pattern.quote(","));
        default -> regex.append(Pattern.quote(String.valueOf(c)));
      }
    }

    private void star(StringBuilder regex) {
      if (this.pos + 1 < this.glob.length() && this.glob.charAt(this.pos + 1) == '*') {
        regex.append(".*");
        this.pos++;
      } else {
        regex.append("[^/]*");
      }
    }

    private void escaped(StringBuilder regex) {
      if (this.pos + 1 >= this.glob.length()) {
        throw this.invalid("trailing backslash");
      }
      this.pos++;
      regex.append(Pattern.quote(String.valueOf(this.glob.charAt(this.pos))));
    }

    private void groupStart(StringBuilder regex) {
      if (this.inGroup) {
        throw this.invalid("nested groups are not supported");
      }
      this.inGroup = true;
      regex.append("(?:");
    }

    private void groupEnd(StringBuilder regex) {
      if (this.inGroup) {
        this.inGroup = false;
        regex.append(')');
      } else {
        regex.append(Pattern.quote("}"));
      }
    }

    /** Appends the character class starting at the current position and moves to its {@code ]}. */
    private void characterClass(StringBuilder regex) {
      int end = this.glob.indexOf(']', this.pos + 2);
      if (end < 0) {
        throw this.invalid("missing ']'");
      }
      String content = this.glob.substring(this.pos + 1, end);
      if (content.startsWith("!")) {
        content = "^" + content.substring(1);
      }
      regex.append('[').append(content.replace("\\", "\\\\").replace("[", "\\[")).append(']');
      this.pos = end;
    }

    private InvalidLinkCheckerConfigException invalid(String reason) {
      return new InvalidLinkCheckerConfigException("Invalid glob '" + this.glob + "': " + reason);
    }
  }
}
