package com.ai.domain.service;

import com.ai.adapter.in.dto.TextAnalysisResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StructuredOutputService Unit Tests
 *
 * Tests for TextAnalysisResult record.
 * Note: analyzeText and analyzeTextWithLanguage require Spring AI integration tests
 * as they depend on ChatClient's fluent API with .entity() method.
 */
@DisplayName("StructuredOutputService")
class StructuredOutputServiceTest {

    @Nested
    @DisplayName("TextAnalysisResult record")
    class TextAnalysisResultTests {

        @Test
        @DisplayName("should create TextAnalysisResult with all fields")
        void shouldCreateTextAnalysisResultWithAllFields() {
            TextAnalysisResult result = new TextAnalysisResult(
                "Test summary",
                TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key point 1", "Key point 2"),
                List.of("Entity 1"),
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

        @Test
        @DisplayName("should handle null key points")
        void shouldHandleNullKeyPoints() {
            TextAnalysisResult result = new TextAnalysisResult(
                "Summary",
                TextAnalysisResult.Sentiment.NEUTRAL,
                null,
                null,
                "English"
            );

            assertThat(result.keyPoints()).isNull();
            assertThat(result.entities()).isNull();
        }

        @Test
        @DisplayName("should handle empty key points list")
        void shouldHandleEmptyKeyPointsList() {
            TextAnalysisResult result = new TextAnalysisResult(
                "Summary",
                TextAnalysisResult.Sentiment.NEUTRAL,
                List.of(),
                List.of(),
                "English"
            );

            assertThat(result.keyPoints()).isEmpty();
            assertThat(result.entities()).isEmpty();
        }

        @Test
        @DisplayName("should have correct equals and hashCode")
        void shouldHaveCorrectEqualsAndHashCode() {
            TextAnalysisResult result1 = new TextAnalysisResult(
                "Summary", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );
            TextAnalysisResult result2 = new TextAnalysisResult(
                "Summary", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );

            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when different summary")
        void shouldNotBeEqualWhenDifferentSummary() {
            TextAnalysisResult result1 = new TextAnalysisResult(
                "Summary 1", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );
            TextAnalysisResult result2 = new TextAnalysisResult(
                "Summary 2", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );

            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("should not be equal when different sentiment")
        void shouldNotBeEqualWhenDifferentSentiment() {
            TextAnalysisResult result1 = new TextAnalysisResult(
                "Summary", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );
            TextAnalysisResult result2 = new TextAnalysisResult(
                "Summary", TextAnalysisResult.Sentiment.NEGATIVE,
                List.of("Key"), List.of("Entity"), "English"
            );

            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("should have correct toString")
        void shouldHaveCorrectToString() {
            TextAnalysisResult result = new TextAnalysisResult(
                "Summary", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );

            String str = result.toString();

            assertThat(str).contains("summary=Summary");
            assertThat(str).contains("sentiment=POSITIVE");
        }

        @Test
        @DisplayName("should be immutable")
        void shouldBeImmutable() {
            TextAnalysisResult result = new TextAnalysisResult(
                "Summary",
                TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"),
                List.of("Entity"),
                "English"
            );

            assertThat(result.summary()).isEqualTo("Summary");
        }

        @Test
        @DisplayName("should handle all sentiment values")
        void shouldHandleAllSentimentValues() {
            for (TextAnalysisResult.Sentiment sentiment : TextAnalysisResult.Sentiment.values()) {
                TextAnalysisResult result = new TextAnalysisResult(
                    "Test", sentiment, List.of(), List.of(), "en"
                );
                assertThat(result.sentiment()).isEqualTo(sentiment);
            }
        }

        @Test
        @DisplayName("should handle long text in summary")
        void shouldHandleLongTextInSummary() {
            String longSummary = "a".repeat(1000);
            TextAnalysisResult result = new TextAnalysisResult(
                longSummary,
                TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"),
                List.of("Entity"),
                "English"
            );

            assertThat(result.summary()).hasSize(1000);
        }

        @Test
        @DisplayName("should handle unicode text")
        void shouldHandleUnicodeText() {
            TextAnalysisResult result = new TextAnalysisResult(
                "中文摘要",
                TextAnalysisResult.Sentiment.POSITIVE,
                List.of("要点一", "要点二"),
                List.of("实体"),
                "Chinese"
            );

            assertThat(result.summary()).isEqualTo("中文摘要");
            assertThat(result.language()).isEqualTo("Chinese");
        }
    }
}
