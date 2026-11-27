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
    StringBuilder regex = new StringBuilder();
    boolean inGroup = false;
    int i = 0;
    while (i < glob.length()) {
      char c = glob.charAt(i);
      switch (c) {
        case '*' -> {
          if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
            regex.append(".*");
            i++;
          } else {
            regex.append("[^/]*");
          }
        }
        case '?' -> regex.append("[^/]");
        case '\\' -> {
          if (i + 1 >= glob.length()) {
            throw new InvalidLinkCheckerConfigException(
                "Invalid glob '" + glob + "': trailing backslash");
          }
          i++;
          regex.append(Pattern.quote(String.valueOf(glob.charAt(i))));
        }
        case '[' -> i = appendCharacterClass(glob, i, regex);
        case '{' -> {
          if (inGroup) {
            throw new InvalidLinkCheckerConfigException(
                "Invalid glob '" + glob + "': nested groups are not supported");
          }
          inGroup = true;
          regex.append("(?:");
        }
        case '}' -> {
          if (inGroup) {
            inGroup = false;
            regex.append(')');
          } else {
            regex.append(Pattern.quote("}"));
          }
        }
        case ',' -> regex.append(inGroup ? "|" : Pattern.quote(","));
        default -> regex.append(Pattern.quote(String.valueOf(c)));
      }
      i++;
    }
    if (inGroup) {
      throw new InvalidLinkCheckerConfigException("Invalid glob '" + glob + "': missing '}'");
    }
    return regex.toString();
  }

  /** Appends the character class starting at {@code start}, returns the index of its {@code ]}. */
  private static int appendCharacterClass(String glob, int start, StringBuilder regex) {
    int end = glob.indexOf(']', start + 2);
    if (end < 0) {
      throw new InvalidLinkCheckerConfigException("Invalid glob '" + glob + "': missing ']'");
    }
    String content = glob.substring(start + 1, end);
    if (content.startsWith("!")) {
      content = "^" + content.substring(1);
    }
    regex.append('[').append(content.replace("\\", "\\\\").replace("[", "\\[")).append(']');
    return end;
  }
}
