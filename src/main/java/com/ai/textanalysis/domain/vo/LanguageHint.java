package com.ai.textanalysis.domain.vo;

public record LanguageHint(String language) {

  private static final String DEFAULT = "English";

  /** Returns a hint with no language. */
  public static LanguageHint none() {
    return new LanguageHint(null);
  }

  /** Creates a trimmed hint, or an unspecified hint when the language is blank. */
  public static LanguageHint of(String language) {
    if (language == null || language.isBlank()) {
      return none();
    }
    return new LanguageHint(language.trim());
  }

  /** Tells whether a language was given. */
  public boolean isSpecified() {
    return language != null && !language.isBlank();
  }

  /** Returns the answer language, or the default. */
  public String responseLanguage() {
    return isSpecified() ? language : DEFAULT;
  }
}
