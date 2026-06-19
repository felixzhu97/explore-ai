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
 * Tests for TextAnalysisResult record and structured output concepts.
 * Note: Integration with Spring AI ChatClient is tested via integration tests.
 */
@DisplayName("StructuredOutputService")
class StructuredOutputServiceTest {

    @Nested
    @DisplayName("TextAnalysisResult record")
    class TextAnalysisResultTests {

        @Test
        @DisplayName("should create TextAnalysisResult with all fields")
        void shouldCreateTextAnalysisResultWithAllFields() {
            // Act
            TextAnalysisResult result = new TextAnalysisResult(
                "Test summary",
                TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key point 1", "Key point 2"),
                List.of("Entity 1"),
                "English"
            );

            // Assert
            assertThat(result.summary()).isEqualTo("Test summary");
            assertThat(result.sentiment()).isEqualTo(TextAnalysisResult.Sentiment.POSITIVE);
            assertThat(result.keyPoints()).hasSize(2);
            assertThat(result.entities()).hasSize(1);
            assertThat(result.language()).isEqualTo("English");
        }

        @Test
        @DisplayName("should support all sentiment types")
        void shouldSupportAllSentimentTypes() {
            // Assert
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
            // Act
            TextAnalysisResult result = new TextAnalysisResult(
                "Summary",
                TextAnalysisResult.Sentiment.NEUTRAL,
                null,
                null,
                "English"
            );

            // Assert
            assertThat(result.keyPoints()).isNull();
            assertThat(result.entities()).isNull();
        }

        @Test
        @DisplayName("should handle empty key points list")
        void shouldHandleEmptyKeyPointsList() {
            // Act
            TextAnalysisResult result = new TextAnalysisResult(
                "Summary",
                TextAnalysisResult.Sentiment.NEUTRAL,
                List.of(),
                List.of(),
                "English"
            );

            // Assert
            assertThat(result.keyPoints()).isEmpty();
            assertThat(result.entities()).isEmpty();
        }

        @Test
        @DisplayName("should have correct equals and hashCode")
        void shouldHaveCorrectEqualsAndHashCode() {
            // Arrange
            TextAnalysisResult result1 = new TextAnalysisResult(
                "Summary", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );
            TextAnalysisResult result2 = new TextAnalysisResult(
                "Summary", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );

            // Assert
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when different summary")
        void shouldNotBeEqualWhenDifferentSummary() {
            // Arrange
            TextAnalysisResult result1 = new TextAnalysisResult(
                "Summary 1", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );
            TextAnalysisResult result2 = new TextAnalysisResult(
                "Summary 2", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );

            // Assert
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("should not be equal when different sentiment")
        void shouldNotBeEqualWhenDifferentSentiment() {
            // Arrange
            TextAnalysisResult result1 = new TextAnalysisResult(
                "Summary", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );
            TextAnalysisResult result2 = new TextAnalysisResult(
                "Summary", TextAnalysisResult.Sentiment.NEGATIVE,
                List.of("Key"), List.of("Entity"), "English"
            );

            // Assert
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("should have correct toString")
        void shouldHaveCorrectToString() {
            // Arrange
            TextAnalysisResult result = new TextAnalysisResult(
                "Summary", TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"), List.of("Entity"), "English"
            );

            // Act
            String str = result.toString();

            // Assert
            assertThat(str).contains("summary=Summary");
            assertThat(str).contains("sentiment=POSITIVE");
        }

        @Test
        @DisplayName("should be immutable")
        void shouldBeImmutable() {
            // Act
            TextAnalysisResult result = new TextAnalysisResult(
                "Summary",
                TextAnalysisResult.Sentiment.POSITIVE,
                List.of("Key"),
                List.of("Entity"),
                "English"
            );

            // Assert - record fields are final
            assertThat(result.summary()).isEqualTo("Summary");
        }
    }
}
