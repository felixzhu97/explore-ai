package com.ai.chat.domain.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Primary language of user input, read from its character distribution: {@code zh}, {@code ja},
 * {@code en}, or {@code default} when no language dominates.
 */
public record DetectedLanguage(String code) {

  private static final Pattern CJK_UNIFIED_IDEOGRAPHS = Pattern.compile("[\\u4e00-\\u9fff]");
  private static final Pattern HIRAGANA = Pattern.compile("[\\u3040-\\u309f]");
  private static final Pattern KATAKANA = Pattern.compile("[\\u30a0-\\u30ff]");
  private static final Pattern KANJI = Pattern.compile("[\\u3400-\\u4dbf\\u4e00-\\u9fff]");
  private static final Pattern LATIN = Pattern.compile("[a-zA-Z]");

  private static final double JAPANESE_KANA_THRESHOLD = 0.05;
  private static final double CHINESE_CJK_THRESHOLD = 0.3;
  private static final double ENGLISH_LATIN_THRESHOLD = 0.5;

  private static final DetectedLanguage DEFAULT = new DetectedLanguage("default");

  /** Detects the primary language of {@code text}. */
  public static DetectedLanguage of(String text) {
    if (text == null || text.isBlank()) {
      return DEFAULT;
    }
    int totalChars = text.length();
    int cjkCount = countMatches(text, CJK_UNIFIED_IDEOGRAPHS);
    int kanaCount = countMatches(text, HIRAGANA) + countMatches(text, KATAKANA);
    int kanjiCount = countMatches(text, KANJI);
    int latinCount = countMatches(text, LATIN);

    boolean japanese =
        (kanaCount > 0 || kanjiCount > 0) && kanaCount > totalChars * JAPANESE_KANA_THRESHOLD;
    if (japanese) {
      return new DetectedLanguage("ja");
    }
    if (cjkCount > totalChars * CHINESE_CJK_THRESHOLD) {
      return new DetectedLanguage("zh");
    }
    if (latinCount > totalChars * ENGLISH_LATIN_THRESHOLD) {
      return new DetectedLanguage("en");
    }
    return DEFAULT;
  }

  private static int countMatches(String text, Pattern pattern) {
    Matcher matcher = pattern.matcher(text);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }
}
