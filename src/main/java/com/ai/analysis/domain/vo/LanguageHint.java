package com.ai.analysis.domain.vo;

/** Documentation. */
public record LanguageHint(String language) {

  private static final String DEFAULT = "English";

  /** Documentation. */
  public static LanguageHint none() {
    return new LanguageHint(null);
  }

  /** Documentation. */
  public static LanguageHint of(String language) {
    if (language == null || language.isBlank()) {
      return none();
    }
    return new LanguageHint(language.trim());
  }

  public boolean isSpecified() {
    return language != null && !language.isBlank();
  }

  /** Documentation. */
  public String responseLanguage() {
    return isSpecified() ? language : DEFAULT;
  }
}
