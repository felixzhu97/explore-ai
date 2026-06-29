package com.ai.rag.infrastructure.llm;

import com.ai.rag.domain.exception.RagServiceException;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaEmbeddingAdapter")
class OllamaEmbeddingAdapterTest {

    @Mock
    private EmbeddingModel embeddingModel;

    private OllamaEmbeddingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OllamaEmbeddingAdapter(embeddingModel, 768);
    }

    @Nested
    @DisplayName("embed()")
    class Embed {

        @Test
        @DisplayName("should delegate to embedding model")
        void shouldDelegateToEmbeddingModel() {
            float[] expectedEmbedding = new float[]{0.1f, 0.2f, 0.3f};

            EmbeddingResponse mockResponse = mock(EmbeddingResponse.class);
            when(mockResponse.getResults()).thenReturn(List.of());
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(mockResponse);

            try {
                adapter.embed("Hello world");
            } catch (RagServiceException e) {
                // Expected for empty results
            }

            verify(embeddingModel).call(any(EmbeddingRequest.class));
        }

        @Test
        @DisplayName("should throw exception when results are empty")
        void shouldThrowExceptionWhenResultsAreEmpty() {
            EmbeddingResponse mockResponse = mock(EmbeddingResponse.class);
            when(mockResponse.getResults()).thenReturn(List.of());
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(mockResponse);

            assertThatThrownBy(() -> adapter.embed("Test"))
                    .isInstanceOf(RagServiceException.class)
                    .hasMessageContaining("Empty embedding response");
        }

        @Test
        @DisplayName("should throw exception when results are null")
        void shouldThrowExceptionWhenResultsAreNull() {
            EmbeddingResponse mockResponse = mock(EmbeddingResponse.class);
            when(mockResponse.getResults()).thenReturn(null);
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(mockResponse);

            assertThatThrownBy(() -> adapter.embed("Test"))
                    .isInstanceOf(RagServiceException.class)
                    .hasMessageContaining("Empty embedding response");
        }

        @Test
        @DisplayName("should propagate RagServiceException")
        void shouldPropagateRagServiceException() {
            when(embeddingModel.call(any(EmbeddingRequest.class)))
                    .thenThrow(new RagServiceException("Service error"));

            assertThatThrownBy(() -> adapter.embed("Test"))
                    .isInstanceOf(RagServiceException.class)
                    .hasMessage("Service error");
        }

        @Test
        @DisplayName("should wrap generic exception in RagServiceException")
        void shouldWrapGenericException() {
            when(embeddingModel.call(any(EmbeddingRequest.class)))
                    .thenThrow(new RuntimeException("Network error"));

            assertThatThrownBy(() -> adapter.embed("Test"))
                    .isInstanceOf(RagServiceException.class)
                    .hasMessageContaining("Embedding generation failed")
                    .hasCauseInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("embedBatch()")
    class EmbedBatch {

        @Test
        @DisplayName("should delegate batch to embedding model")
        void shouldDelegateBatchToEmbeddingModel() {
            EmbeddingResponse mockResponse = mock(EmbeddingResponse.class);
            when(mockResponse.getResults()).thenReturn(List.of());
            when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(mockResponse);

            try {
                adapter.embedBatch(List.of("Text 1", "Text 2"));
            } catch (RagServiceException e) {
                // Expected for empty results
            }

            verify(embeddingModel).call(any(EmbeddingRequest.class));
        }

        @Test
        @DisplayName("should throw exception on batch failure")
        void shouldThrowExceptionOnBatchFailure() {
            when(embeddingModel.call(any(EmbeddingRequest.class)))
                    .thenThrow(new RuntimeException("Batch error"));

            assertThatThrownBy(() -> adapter.embedBatch(List.of("Text 1", "Text 2")))
                    .isInstanceOf(RagServiceException.class)
                    .hasMessageContaining("Batch embedding generation failed");
        }
    }

    @Nested
    @DisplayName("getDimensions()")
    class GetDimensions {

        @Test
        @DisplayName("should return configured dimensions")
        void shouldReturnConfiguredDimensions() {
            assertThat(adapter.getDimensions()).isEqualTo(768);
        }
    }
}
