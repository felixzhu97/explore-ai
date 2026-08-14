package com.ai.analysis.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.analysis.domain.exception.InvalidAnalysisTextException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnalysisText")
class AnalysisTextTest {

  @Test
  @DisplayName("should create from valid text")
  void shouldCreateFromValidText() {
    AnalysisText text = AnalysisText.of("  Hello world  ");

    assertThat(text.value()).isEqualTo("Hello world");
  }

  @Test
  @DisplayName("should reject blank text")
  void shouldRejectBlankText() {
    assertThatThrownBy(() -> AnalysisText.of("   "))
        .isInstanceOf(InvalidAnalysisTextException.class)
        .hasMessageContaining("blank");
  }

  @Test
  @DisplayName("should reject text exceeding max length")
  void shouldRejectTextExceedingMaxLength() {
    assertThatThrownBy(() -> AnalysisText.of("a".repeat(50_001)))
        .isInstanceOf(InvalidAnalysisTextException.class)
        .hasMessageContaining("maximum length");
  }

  @Test
  @DisplayName("should reject null via compact constructor")
  void shouldRejectNullViaCompactConstructor() {
    assertThatThrownBy(() -> new AnalysisText(null))
        .isInstanceOf(InvalidAnalysisTextException.class)
        .hasMessageContaining("blank");
  }

  @Test
  @DisplayName("should build prompt when text contains percent sign")
  void shouldBuildPromptWhenTextContainsPercentSign() {
    AnalysisText text = AnalysisText.of("90% complete");

    String prompt = text.buildAnalysisPrompt(LanguageHint.none());

    assertThat(prompt).contains("90% complete");
    assertThat(prompt).contains("Text: 90% complete");
  }

  @Test
  @DisplayName("should build analysis prompt without language hint")
  void shouldBuildAnalysisPromptWithoutLanguageHint() {
    AnalysisText text = AnalysisText.of("Sample");

    String prompt = text.buildAnalysisPrompt(LanguageHint.none());

    assertThat(prompt).contains("Sample");
    assertThat(prompt).contains("POSITIVE, NEUTRAL, or NEGATIVE");
    assertThat(prompt).doesNotContain("Please respond in");
  }

  @Test
  @DisplayName("should build analysis prompt with language hint")
  void shouldBuildAnalysisPromptWithLanguageHint() {
    AnalysisText text = AnalysisText.of("Sample");

    String prompt = text.buildAnalysisPrompt(LanguageHint.of("Chinese"));

    assertThat(prompt).contains("Please respond in Chinese.");
  }
}
