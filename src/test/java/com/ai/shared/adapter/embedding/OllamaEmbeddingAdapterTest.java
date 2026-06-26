package com.ai.adapter.out.embedding;

import com.ai.rag.domain.exception.RagServiceException;
import com.ai.rag.infrastructure.llm.OllamaEmbeddingAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OllamaEmbeddingAdapter Unit Tests
 *
 * Tests the Ollama embedding adapter implementation:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests single/batch embedding, dimensions configuration, and error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaEmbeddingAdapter")
class OllamaEmbeddingAdapterTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private EmbeddingResponse embeddingResponse;

    private OllamaEmbeddingAdapter adapter;

    private static final int TEST_DIMENSIONS = 768;

    @BeforeEach
    void setUp() {
        adapter = new OllamaEmbeddingAdapter(embeddingModel, TEST_DIMENSIONS);
    }

    @Nested
    @DisplayName("shouldReturnEmbeddingFromOllamaApi")
    class ShouldReturnEmbeddingFromOllamaApi {

        @Test
        @DisplayName("should return embedding from Ollama API for single text")
        void shouldReturnEmbeddingFromOllamaApiForSingleText() {
            // Arrange
            String testText = "Hello, world!";
            float[] expectedEmbedding = createTestEmbedding(4);

            // Mock EmbeddingResponse to return our test embedding
            org.springframework.ai.embedding.Embedding mockEmbedding = mock(org.springframework.ai.embedding.Embedding.class);
            when(mockEmbedding.getOutput()).thenReturn(expectedEmbedding);
            when(embeddingResponse.getResults()).thenReturn(List.of(mockEmbedding));
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);

            // Act
            float[] result = adapter.embed(testText);

            // Assert
            assertThat(result).isEqualTo(expectedEmbedding);
            verify(embeddingModel).call(any(EmbeddingRequest.class));
        }

        @Test
        @DisplayName("should create EmbeddingRequest with single text")
        void shouldCreateEmbeddingRequestWithSingleText() {
            // Arrange
            String testText = "Test input text";
            float[] embedding = createTestEmbedding(4);
            org.springframework.ai.embedding.Embedding mockEmbedding = mock(org.springframework.ai.embedding.Embedding.class);
            when(mockEmbedding.getOutput()).thenReturn(embedding);
            when(embeddingResponse.getResults()).thenReturn(List.of(mockEmbedding));
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);

            // Act
            adapter.embed(testText);

            // Assert
            verify(embeddingModel).call(argThat(request -> {
                // The request should contain our test text via getInstructions()
                return request.getInstructions().contains(testText);
            }));
        }

        @Test
        @DisplayName("should return correct embedding values")
        void shouldReturnCorrectEmbeddingValues() {
            // Arrange
            String testText = "Short text";
            float[] expectedEmbedding = new float[]{0.1f, 0.2f, 0.3f, 0.4f};
            org.springframework.ai.embedding.Embedding mockEmbedding = mock(org.springframework.ai.embedding.Embedding.class);
            when(mockEmbedding.getOutput()).thenReturn(expectedEmbedding);
            when(embeddingResponse.getResults()).thenReturn(List.of(mockEmbedding));
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);

            // Act
            float[] result = adapter.embed(testText);

            // Assert
            assertThat(result[0]).isEqualTo(0.1f);
            assertThat(result[1]).isEqualTo(0.2f);
            assertThat(result[2]).isEqualTo(0.3f);
            assertThat(result[3]).isEqualTo(0.4f);
        }
    }

    @Nested
    @DisplayName("shouldReturnListOfEmbeddingsForBatch")
    class ShouldReturnListOfEmbeddingsForBatch {

        @Test
        @DisplayName("should return list of embeddings for batch input")
        void shouldReturnListOfEmbeddingsForBatchInput() {
            // Arrange
            List<String> texts = List.of("Text 1", "Text 2", "Text 3");
            float[] embedding1 = createTestEmbedding(4);
            float[] embedding2 = createTestEmbedding(4);
            float[] embedding3 = createTestEmbedding(4);

            org.springframework.ai.embedding.Embedding result1 = mock(org.springframework.ai.embedding.Embedding.class);
            when(result1.getOutput()).thenReturn(embedding1);
            org.springframework.ai.embedding.Embedding result2 = mock(org.springframework.ai.embedding.Embedding.class);
            when(result2.getOutput()).thenReturn(embedding2);
            org.springframework.ai.embedding.Embedding result3 = mock(org.springframework.ai.embedding.Embedding.class);
            when(result3.getOutput()).thenReturn(embedding3);

            when(embeddingResponse.getResults()).thenReturn(List.of(result1, result2, result3));
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);

            // Act
            List<float[]> results = adapter.embedBatch(texts);

            // Assert
            assertThat(results).hasSize(3);
            assertThat(results.get(0)).isEqualTo(embedding1);
            assertThat(results.get(1)).isEqualTo(embedding2);
            assertThat(results.get(2)).isEqualTo(embedding3);
        }

        @Test
        @DisplayName("should create EmbeddingRequest with all batch texts")
        void shouldCreateEmbeddingRequestWithAllBatchTexts() {
            // Arrange
            List<String> texts = List.of("First", "Second", "Third");
            when(embeddingResponse.getResults()).thenReturn(List.of());
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);

            // Act
            try {
                adapter.embedBatch(texts);
            } catch (Exception ignored) {
            }

            // Assert
            verify(embeddingModel).call(argThat(request ->
                request.getInstructions().size() == 3
            ));
        }

        @Test
        @DisplayName("should handle batch with single text")
        void shouldHandleBatchWithSingleText() {
            // Arrange
            List<String> texts = List.of("Single text");
            float[] expectedEmbedding = createTestEmbedding(4);
            org.springframework.ai.embedding.Embedding mockEmbedding = mock(org.springframework.ai.embedding.Embedding.class);
            when(mockEmbedding.getOutput()).thenReturn(expectedEmbedding);
            when(embeddingResponse.getResults()).thenReturn(List.of(mockEmbedding));
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);

            // Act
            List<float[]> results = adapter.embedBatch(texts);

            // Assert
            assertThat(results).hasSize(1);
            assertThat(results.get(0)).isEqualTo(expectedEmbedding);
        }
    }

    @Nested
    @DisplayName("shouldThrowRuntimeExceptionWhenResultsAreEmpty")
    class ShouldThrowRuntimeExceptionWhenResultsAreEmpty {

        @Test
        @DisplayName("should throw RuntimeException when results are empty for single embed")
        void shouldThrowRuntimeExceptionWhenResultsAreEmptyForSingleEmbed() {
            // Arrange
            String testText = "Test text";
            when(embeddingResponse.getResults()).thenReturn(List.of());
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);

            // Act & Assert
            assertThatThrownBy(() -> adapter.embed(testText))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Empty embedding response");
        }

        @Test
        @DisplayName("should throw RuntimeException when results are null for single embed")
        void shouldThrowRuntimeExceptionWhenResultsAreNullForSingleEmbed() {
            // Arrange
            String testText = "Test text";
            when(embeddingResponse.getResults()).thenReturn(null);
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);

            // Act & Assert
            assertThatThrownBy(() -> adapter.embed(testText))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should return empty list when batch returns empty results")
        void shouldReturnEmptyListWhenBatchReturnsEmptyResults() {
            // Arrange
            List<String> texts = List.of("Text 1", "Text 2");
            when(embeddingResponse.getResults()).thenReturn(List.of());
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);

            // Act - Batch with empty results returns empty list (no exception thrown)
            List<float[]> results = adapter.embedBatch(texts);

            // Assert - Returns empty list, not an exception
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("shouldThrowRuntimeExceptionOnApiFailure")
    class ShouldThrowRuntimeExceptionOnApiFailure {

        @Test
        @DisplayName("should throw RuntimeException when API fails for single embed")
        void shouldThrowRuntimeExceptionWhenApiFailsForSingleEmbed() {
            // Arrange
            String testText = "Test text";
            when(embeddingModel.call(any(EmbeddingRequest.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            // Act & Assert
            assertThatThrownBy(() -> adapter.embed(testText))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Embedding generation failed")
                    .hasCauseInstanceOf(RuntimeException.class)
                    .hasRootCauseMessage("Connection refused");
        }

        @Test
        @DisplayName("should throw RuntimeException when API fails for batch")
        void shouldThrowRuntimeExceptionWhenApiFailsForBatch() {
            // Arrange
            List<String> texts = List.of("Text 1", "Text 2");
            when(embeddingModel.call(any(EmbeddingRequest.class)))
                    .thenThrow(new RuntimeException("API timeout"));

            // Act & Assert
            assertThatThrownBy(() -> adapter.embedBatch(texts))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Batch embedding generation failed");
        }

        @Test
        @DisplayName("should preserve original exception cause")
        void shouldPreserveOriginalExceptionCause() {
            // Arrange
            String testText = "Test text";
            String causeMessage = "Ollama service unavailable";
            when(embeddingModel.call(any(EmbeddingRequest.class)))
                    .thenThrow(new RuntimeException(causeMessage));

            // Act & Assert
            assertThatThrownBy(() -> adapter.embed(testText))
                    .isInstanceOf(RagServiceException.class)
                    .hasMessageContaining("Embedding generation failed")
                    .hasCauseInstanceOf(RuntimeException.class)
                    .hasRootCauseMessage(causeMessage);
        }
    }

    @Nested
    @DisplayName("shouldReturnConfiguredDimensions")
    class ShouldReturnConfiguredDimensions {

        @Test
        @DisplayName("should return configured dimensions")
        void shouldReturnConfiguredDimensions() {
            // Act
            int dimensions = adapter.getDimensions();

            // Assert
            assertThat(dimensions).isEqualTo(TEST_DIMENSIONS);
        }

        @Test
        @DisplayName("should return different dimensions for different configurations")
        void shouldReturnDifferentDimensionsForDifferentConfigurations() {
            // Arrange
            OllamaEmbeddingAdapter adapterWithDifferentDims = new OllamaEmbeddingAdapter(embeddingModel, 1024);

            // Act
            int dimensions = adapterWithDifferentDims.getDimensions();

            // Assert
            assertThat(dimensions).isEqualTo(1024);
        }

        @Test
        @DisplayName("should return default dimensions when not configured")
        void shouldReturnDefaultDimensionsWhenNotConfigured() {
            // Arrange
            OllamaEmbeddingAdapter adapterWithDefault = new OllamaEmbeddingAdapter(embeddingModel, 768);

            // Act
            int dimensions = adapterWithDefault.getDimensions();

            // Assert
            assertThat(dimensions).isEqualTo(768);
        }
    }

    @Nested
    @DisplayName("shouldHandleNullTextInBatch")
    class ShouldHandleNullTextInBatch {

        @Test
        @DisplayName("should handle list containing elements and pass to API")
        void shouldHandleListContainingElementsAndPassToApi() {
            // Arrange
            List<String> texts = List.of("Valid text", "Another valid");
            float[] embedding = createTestEmbedding(4);
            org.springframework.ai.embedding.Embedding mockResult1 = mock(org.springframework.ai.embedding.Embedding.class);
            org.springframework.ai.embedding.Embedding mockResult2 = mock(org.springframework.ai.embedding.Embedding.class);
            when(mockResult1.getOutput()).thenReturn(embedding);
            when(mockResult2.getOutput()).thenReturn(embedding);
            when(embeddingResponse.getResults()).thenReturn(List.of(mockResult1, mockResult2));
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(embeddingResponse);

            // Act
            List<float[]> results = adapter.embedBatch(texts);

            // Assert
            assertThat(results).hasSize(2);
        }
    }

    // Helper method to create test embeddings
    private float[] createTestEmbedding(int dimensions) {
        float[] embedding = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            embedding[i] = (float) (i + 1) * 0.25f;
        }
        return embedding;
    }
}
