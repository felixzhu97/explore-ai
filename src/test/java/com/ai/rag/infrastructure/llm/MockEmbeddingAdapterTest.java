package com.ai.rag.infrastructure.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MockEmbeddingAdapter")
class MockEmbeddingAdapterTest {

    private MockEmbeddingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockEmbeddingAdapter();
    }

    @Nested
    @DisplayName("embed()")
    class Embed {

        @Test
        @DisplayName("should return embedding array with correct dimensions")
        void shouldReturnEmbeddingWithCorrectDimensions() {
            float[] embedding = adapter.embed("Hello world");

            assertThat(embedding).hasSize(1536);
        }

        @Test
        @DisplayName("should return values between -1 and 1")
        void shouldReturnValuesBetweenMinusOneAndOne() {
            float[] embedding = adapter.embed("Test text");

            for (float value : embedding) {
                assertThat(value).isBetween(-1.0f, 1.0f);
            }
        }

        @Test
        @DisplayName("should return different embeddings for different texts")
        void shouldReturnDifferentEmbeddingsForDifferentTexts() {
            float[] embedding1 = adapter.embed("Hello");
            float[] embedding2 = adapter.embed("World");

            assertThat(embedding1).isNotEqualTo(embedding2);
        }

        @RepeatedTest(5)
        @DisplayName("should return different embeddings on repeated calls")
        void shouldReturnDifferentEmbeddingsOnRepeatedCalls() {
            float[] embedding1 = adapter.embed("Same text");
            float[] embedding2 = adapter.embed("Same text");

            assertThat(embedding1).isNotEqualTo(embedding2);
        }

        @Test
        @DisplayName("should handle empty string")
        void shouldHandleEmptyString() {
            float[] embedding = adapter.embed("");

            assertThat(embedding).hasSize(1536);
        }

        @Test
        @DisplayName("should handle long text")
        void shouldHandleLongText() {
            String longText = "A".repeat(10000);

            float[] embedding = adapter.embed(longText);

            assertThat(embedding).hasSize(1536);
        }
    }

    @Nested
    @DisplayName("embedBatch()")
    class EmbedBatch {

        @Test
        @DisplayName("should return embeddings for all texts")
        void shouldReturnEmbeddingsForAllTexts() {
            List<String> texts = List.of("Text 1", "Text 2", "Text 3");

            List<float[]> embeddings = adapter.embedBatch(texts);

            assertThat(embeddings).hasSize(3);
            for (float[] embedding : embeddings) {
                assertThat(embedding).hasSize(1536);
            }
        }

        @Test
        @DisplayName("should return empty list for empty input")
        void shouldReturnEmptyListForEmptyInput() {
            List<String> texts = List.of();

            List<float[]> embeddings = adapter.embedBatch(texts);

            assertThat(embeddings).isEmpty();
        }

        @Test
        @DisplayName("should handle single item list")
        void shouldHandleSingleItemList() {
            List<String> texts = List.of("Single text");

            List<float[]> embeddings = adapter.embedBatch(texts);

            assertThat(embeddings).hasSize(1);
            assertThat(embeddings.get(0)).hasSize(1536);
        }

        @Test
        @DisplayName("should return different embeddings for batch items")
        void shouldReturnDifferentEmbeddingsForBatchItems() {
            List<String> texts = List.of("Item 1", "Item 2");

            List<float[]> embeddings = adapter.embedBatch(texts);

            assertThat(embeddings.get(0)).isNotEqualTo(embeddings.get(1));
        }
    }

    @Nested
    @DisplayName("getDimensions()")
    class GetDimensions {

        @Test
        @DisplayName("should return correct dimensions")
        void shouldReturnCorrectDimensions() {
            assertThat(adapter.getDimensions()).isEqualTo(1536);
        }
    }
}
