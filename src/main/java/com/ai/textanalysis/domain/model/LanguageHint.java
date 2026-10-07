package com.ai.textanalysis.domain.model;

public record LanguageHint(String language) {

  private static final String DEFAULT = "English";

  /** Returns a hint with no language. */
  public static LanguageHint createEmptyHint() {
    return new LanguageHint(null);
  }

  /** Creates a trimmed hint, or an unspecified hint when the language is blank. */
  public static LanguageHint createHint(String language) {
    if (language == null || language.isBlank()) {
      return createEmptyHint();
    }
    return new LanguageHint(language.trim());
  }

  /** Tells whether a language was given. */
  public boolean isSpecified() {
    return language != null && !language.isBlank();
  }

  /** Returns the answer language, or the default. */
  public String getResponseLanguage() {
    return isSpecified() ? language : DEFAULT;
  }
}
