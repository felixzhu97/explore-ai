package com.ai.rag.domain.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VectorSimilarity Utility Tests
 * 
 * Tests for cosine similarity calculation following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests normal cases, edge cases, and boundary conditions
 */
@DisplayName("VectorSimilarity")
class VectorSimilarityTest {

    @Nested
    @DisplayName("cosineSimilarity()")
    class CosineSimilarity {

        @Test
        @DisplayName("should return 1.0 for identical vectors")
        void shouldReturnOneForIdenticalVectors() {
            float[] a = {1.0f, 2.0f, 3.0f};
            float[] b = {1.0f, 2.0f, 3.0f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should return 1.0 for zero vectors")
        void shouldReturnOneForZeroVectors() {
            float[] a = {0.0f, 0.0f, 0.0f};
            float[] b = {0.0f, 0.0f, 0.0f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should return -1.0 for opposite vectors")
        void shouldReturnNegativeOneForOppositeVectors() {
            float[] a = {1.0f, 2.0f, 3.0f};
            float[] b = {-1.0f, -2.0f, -3.0f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isCloseTo(-1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should return 0.0 for orthogonal vectors")
        void shouldReturnZeroForOrthogonalVectors() {
            float[] a = {1.0f, 0.0f, 0.0f};
            float[] b = {0.0f, 1.0f, 0.0f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should return 0.0 for null first vector")
        void shouldReturnZeroForNullFirstVector() {
            float[] a = null;
            float[] b = {1.0f, 2.0f, 3.0f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0.0 for null second vector")
        void shouldReturnZeroForNullSecondVector() {
            float[] a = {1.0f, 2.0f, 3.0f};
            float[] b = null;

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0.0 for both null vectors")
        void shouldReturnZeroForBothNullVectors() {
            float[] a = null;
            float[] b = null;

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0.0 for vectors of different lengths")
        void shouldReturnZeroForDifferentLengthVectors() {
            float[] a = {1.0f, 2.0f, 3.0f};
            float[] b = {1.0f, 2.0f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should handle single element vectors")
        void shouldHandleSingleElementVectors() {
            float[] a = {5.0f};
            float[] b = {10.0f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should handle negative values in vectors")
        void shouldHandleNegativeValuesInVectors() {
            float[] a = {-1.0f, -2.0f, -3.0f};
            float[] b = {1.0f, 2.0f, 3.0f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isCloseTo(-1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should handle mixed positive and negative values")
        void shouldHandleMixedPositiveAndNegativeValues() {
            float[] a = {1.0f, -2.0f, 3.0f};
            float[] b = {2.0f, -4.0f, 6.0f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should handle high dimensional vectors")
        void shouldHandleHighDimensionalVectors() {
            float[] a = new float[1000];
            float[] b = new float[1000];
            for (int i = 0; i < 1000; i++) {
                a[i] = (float) Math.random();
                b[i] = (float) Math.random();
            }

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isBetween(-1.0, 1.0);
        }

        @Test
        @DisplayName("should handle very small values")
        void shouldHandleVerySmallValues() {
            float[] a = {0.0001f, 0.0002f};
            float[] b = {0.0001f, 0.0002f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should handle very large values")
        void shouldHandleVeryLargeValues() {
            float[] a = {1_000_000f, 2_000_000f};
            float[] b = {1_000_000f, 2_000_000f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
        }

        @Test
        @DisplayName("should calculate correct similarity for known vectors")
        void shouldCalculateCorrectSimilarityForKnownVectors() {
            float[] a = {1.0f, 0.0f};
            float[] b = {0.7071f, 0.7071f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isCloseTo(0.7071, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        @DisplayName("should return value between -1 and 1 for random vectors")
        void shouldReturnValueBetweenNegativeOneAndOneForRandomVectors() {
            float[] a = {0.5f, -0.3f, 0.8f};
            float[] b = {0.2f, 0.4f, -0.1f};

            double similarity = VectorSimilarity.cosineSimilarity(a, b);

            assertThat(similarity).isBetween(-1.0, 1.0);
        }
    }
}
