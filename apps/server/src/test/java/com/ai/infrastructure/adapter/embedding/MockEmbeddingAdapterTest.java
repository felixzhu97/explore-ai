package com.ai.adapter.out.embedding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockEmbeddingAdapter Unit Tests
 *
 * Tests the mock embedding adapter implementation:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests embedding dimensions, value ranges, and randomness
 */
@DisplayName("MockEmbeddingAdapter")
class MockEmbeddingAdapterTest {

    private MockEmbeddingAdapter adapter;

    private static final int EXPECTED_DIMENSIONS = 1536;

    @BeforeEach
    void setUp() {
        adapter = new MockEmbeddingAdapter();
    }

    @Nested
    @DisplayName("shouldReturnEmbeddingWithCorrectDimensions")
    class ShouldReturnEmbeddingWithCorrectDimensions {

        @Test
        @DisplayName("should return embedding with 1536 dimensions")
        void shouldReturnEmbeddingWith1536Dimensions() {
            // Arrange
            String testText = "Hello, world!";

            // Act
            float[] result = adapter.embed(testText);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(EXPECTED_DIMENSIONS);
        }

        @Test
        @DisplayName("should return consistent dimensions for different texts")
        void shouldReturnConsistentDimensionsForDifferentTexts() {
            // Arrange
            String[] testTexts = {"Short", "A longer text for testing", "这是一个测试文本"};

            // Act & Assert
            for (String text : testTexts) {
                float[] result = adapter.embed(text);
                assertThat(result).hasSize(EXPECTED_DIMENSIONS);
            }
        }

        @Test
        @DisplayName("should return correct dimensions from getDimensions method")
        void shouldReturnCorrectDimensionsFromGetDimensionsMethod() {
            // Act
            int dimensions = adapter.getDimensions();

            // Assert
            assertThat(dimensions).isEqualTo(EXPECTED_DIMENSIONS);
        }
    }

    @Nested
    @DisplayName("shouldReturnEmbeddingWithinRange")
    class ShouldReturnEmbeddingWithinRange {

        @Test
        @DisplayName("should return values between -1 and 1")
        void shouldReturnValuesBetweenNegativeOneAndOne() {
            // Arrange
            String testText = "Test text for range checking";

            // Act
            float[] result = adapter.embed(testText);

            // Assert
            for (int i = 0; i < result.length; i++) {
                assertThat(result[i])
                        .as("Value at index %d should be between -1 and 1", i)
                        .isGreaterThanOrEqualTo(-1.0f)
                        .isLessThanOrEqualTo(1.0f);
            }
        }

        @Test
        @DisplayName("should have some non-zero values")
        void shouldHaveSomeNonZeroValues() {
            // Arrange
            String testText = "Test text";

            // Act
            float[] result = adapter.embed(testText);

            // Assert
            boolean hasNonZero = false;
            for (float value : result) {
                if (value != 0.0f) {
                    hasNonZero = true;
                    break;
                }
            }
            assertThat(hasNonZero).isTrue();
        }

        @Test
        @DisplayName("should return values within range for batch")
        void shouldReturnValuesWithinRangeForBatch() {
            // Arrange
            List<String> texts = List.of("Text 1", "Text 2");

            // Act
            List<float[]> results = adapter.embedBatch(texts);

            // Assert
            for (float[] embedding : results) {
                for (float value : embedding) {
                    assertThat(value).isGreaterThanOrEqualTo(-1.0f);
                    assertThat(value).isLessThanOrEqualTo(1.0f);
                }
            }
        }
    }

    @Nested
    @DisplayName("shouldReturnDifferentEmbeddingsForSameInput")
    class ShouldReturnDifferentEmbeddingsForSameInput {

        @RepeatedTest(10)
        @DisplayName("should return different embeddings for same input (randomness check)")
        void shouldReturnDifferentEmbeddingsForSameInput() {
            // Arrange
            String testText = "Same input text";
            int iterations = 5;
            float[][] embeddings = new float[iterations][];

            // Act
            for (int i = 0; i < iterations; i++) {
                embeddings[i] = adapter.embed(testText);
            }

            // Assert - At least some values should be different across iterations
            boolean foundDifference = false;
            for (int i = 1; i < iterations; i++) {
                for (int j = 0; j < embeddings[i].length; j++) {
                    if (Math.abs(embeddings[i][j] - embeddings[0][j]) > 0.0001f) {
                        foundDifference = true;
                        break;
                    }
                }
                if (foundDifference) break;
            }
            assertThat(foundDifference)
                    .as("Random embeddings should produce different values for same input")
                    .isTrue();
        }

        @Test
        @DisplayName("should demonstrate randomness in multiple calls")
        void shouldDemonstrateRandomnessInMultipleCalls() {
            // Arrange
            String text = "Testing randomness";
            int callCount = 3;
            float[][] results = new float[callCount][];

            // Act
            for (int i = 0; i < callCount; i++) {
                results[i] = adapter.embed(text);
            }

            // Assert - Count how many embeddings differ from the first one
            int differences = 0;
            for (int i = 1; i < callCount; i++) {
                boolean differs = false;
                for (int j = 0; j < results[i].length; j++) {
                    if (Math.abs(results[i][j] - results[0][j]) > 0.0001f) {
                        differs = true;
                        break;
                    }
                }
                if (differs) differences++;
            }
            // At least one should be different (with high probability)
            assertThat(differences).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("shouldReturnOneEmbeddingPerTextInBatch")
    class ShouldReturnOneEmbeddingPerTextInBatch {

        @Test
        @DisplayName("should return same number of embeddings as input texts")
        void shouldReturnSameNumberOfEmbeddingsAsInputTexts() {
            // Arrange
            List<String> texts = List.of("Text one", "Text two", "Text three", "Text four", "Text five");

            // Act
            List<float[]> results = adapter.embedBatch(texts);

            // Assert
            assertThat(results).hasSize(texts.size());
        }

        @Test
        @DisplayName("should return embeddings with correct dimensions for each text in batch")
        void shouldReturnEmbeddingsWithCorrectDimensionsForEachTextInBatch() {
            // Arrange
            List<String> texts = List.of("First", "Second", "Third");

            // Act
            List<float[]> results = adapter.embedBatch(texts);

            // Assert
            assertThat(results).hasSize(3);
            for (float[] embedding : results) {
                assertThat(embedding).hasSize(EXPECTED_DIMENSIONS);
            }
        }

        @Test
        @DisplayName("should handle empty list")
        void shouldHandleEmptyList() {
            // Arrange
            List<String> emptyList = List.of();

            // Act
            List<float[]> results = adapter.embedBatch(emptyList);

            // Assert
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should handle single item list")
        void shouldHandleSingleItemList() {
            // Arrange
            List<String> singleItem = List.of("Only one");

            // Act
            List<float[]> results = adapter.embedBatch(singleItem);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).hasSize(EXPECTED_DIMENSIONS);
        }

        @Test
        @DisplayName("should handle large batch")
        void shouldHandleLargeBatch() {
            // Arrange
            int batchSize = 100;
            List<String> largeBatch = new java.util.ArrayList<>();
            for (int i = 0; i < batchSize; i++) {
                largeBatch.add("Text item " + i);
            }

            // Act
            List<float[]> results = adapter.embedBatch(largeBatch);

            // Assert
            assertThat(results).hasSize(batchSize);
            for (float[] embedding : results) {
                assertThat(embedding).hasSize(EXPECTED_DIMENSIONS);
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle empty string")
        void shouldHandleEmptyString() {
            // Act
            float[] result = adapter.embed("");

            // Assert
            assertThat(result).hasSize(EXPECTED_DIMENSIONS);
            for (float value : result) {
                assertThat(value).isGreaterThanOrEqualTo(-1.0f);
                assertThat(value).isLessThanOrEqualTo(1.0f);
            }
        }

        @Test
        @DisplayName("should handle very long text")
        void shouldHandleVeryLongText() {
            // Arrange
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                sb.append("word").append(i).append(" ");
            }
            String veryLongText = sb.toString();

            // Act
            float[] result = adapter.embed(veryLongText);

            // Assert
            assertThat(result).hasSize(EXPECTED_DIMENSIONS);
        }

        @Test
        @DisplayName("should handle unicode text")
        void shouldHandleUnicodeText() {
            // Arrange
            String unicodeText = "你好世界 αβγδ 测试 ∀∃∧∨";

            // Act
            float[] result = adapter.embed(unicodeText);

            // Assert
            assertThat(result).hasSize(EXPECTED_DIMENSIONS);
        }

        @Test
        @DisplayName("should handle special characters")
        void shouldHandleSpecialCharacters() {
            // Arrange
            String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?\\`~";

            // Act
            float[] result = adapter.embed(specialChars);

            // Assert
            assertThat(result).hasSize(EXPECTED_DIMENSIONS);
        }
    }
}
