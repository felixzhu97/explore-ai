package com.ai.analysis.domain.vo;

import com.ai.analysis.domain.exception.InvalidAnalysisTextException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AnalysisText")
class AnalysisTextTest {

    @Test
    @DisplayName("should create from valid text")
    void should_create_from_valid_text() {
        AnalysisText text = AnalysisText.of("  Hello world  ");

        assertThat(text.value()).isEqualTo("Hello world");
    }

    @Test
    @DisplayName("should reject blank text")
    void should_reject_blank_text() {
        assertThatThrownBy(() -> AnalysisText.of("   "))
                .isInstanceOf(InvalidAnalysisTextException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("should reject text exceeding max length")
    void should_reject_text_exceeding_max_length() {
        assertThatThrownBy(() -> AnalysisText.of("a".repeat(50_001)))
                .isInstanceOf(InvalidAnalysisTextException.class)
                .hasMessageContaining("maximum length");
    }

    @Test
    @DisplayName("should reject null via compact constructor")
    void should_reject_null_via_compact_constructor() {
        assertThatThrownBy(() -> new AnalysisText(null))
                .isInstanceOf(InvalidAnalysisTextException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("should build prompt when text contains percent sign")
    void should_build_prompt_when_text_contains_percent_sign() {
        AnalysisText text = AnalysisText.of("90% complete");

        String prompt = text.buildAnalysisPrompt(LanguageHint.none());

        assertThat(prompt).contains("90% complete");
        assertThat(prompt).contains("Text: 90% complete");
    }

    @Test
    @DisplayName("should build analysis prompt without language hint")
    void should_build_analysis_prompt_without_language_hint() {
        AnalysisText text = AnalysisText.of("Sample");

        String prompt = text.buildAnalysisPrompt(LanguageHint.none());

        assertThat(prompt).contains("Sample");
        assertThat(prompt).contains("POSITIVE, NEUTRAL, or NEGATIVE");
        assertThat(prompt).doesNotContain("Please respond in");
    }

    @Test
    @DisplayName("should build analysis prompt with language hint")
    void should_build_analysis_prompt_with_language_hint() {
        AnalysisText text = AnalysisText.of("Sample");

        String prompt = text.buildAnalysisPrompt(LanguageHint.of("Chinese"));

        assertThat(prompt).contains("Please respond in Chinese.");
    }
}
