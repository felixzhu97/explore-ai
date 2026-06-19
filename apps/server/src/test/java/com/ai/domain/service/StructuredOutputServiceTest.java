package com.ai.domain.service;

import com.ai.adapter.in.dto.TextAnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StructuredOutputService")
class StructuredOutputServiceTest {

    @Test
    @DisplayName("should create TextAnalysisResult with all fields")
    void shouldCreateTextAnalysisResultWithAllFields() {
        TextAnalysisResult result = new TextAnalysisResult(
            "Test summary",
            TextAnalysisResult.Sentiment.POSITIVE,
            java.util.List.of("Key point 1", "Key point 2"),
            java.util.List.of("Entity 1"),
            "English"
        );

        assertThat(result.summary()).isEqualTo("Test summary");
        assertThat(result.sentiment()).isEqualTo(TextAnalysisResult.Sentiment.POSITIVE);
        assertThat(result.keyPoints()).hasSize(2);
        assertThat(result.entities()).hasSize(1);
        assertThat(result.language()).isEqualTo("English");
    }

    @Test
    @DisplayName("should support all sentiment types")
    void shouldSupportAllSentimentTypes() {
        assertThat(TextAnalysisResult.Sentiment.values())
            .containsExactlyInAnyOrder(
                TextAnalysisResult.Sentiment.POSITIVE,
                TextAnalysisResult.Sentiment.NEUTRAL,
                TextAnalysisResult.Sentiment.NEGATIVE
            );
    }
}
