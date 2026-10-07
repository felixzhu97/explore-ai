package com.ai.rag.infra.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ai.common.exception.DomainException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaTextEmbeddingGateway")
class OllamaTextEmbeddingGatewayTest {

  @Mock private EmbeddingModel embeddingModel;

  private OllamaTextEmbeddingGateway adapter;

  @BeforeEach
  void setUp() {
    adapter = new OllamaTextEmbeddingGateway(embeddingModel, 1024);
  }

  @Test
  @DisplayName("should return the embedding of the text")
  void shouldReturnTheEmbeddingOfTheText() {
    when(embeddingModel.call(any(EmbeddingRequest.class)))
        .thenReturn(new EmbeddingResponse(List.of(new Embedding(new float[] {0.1f, 0.2f}, 0))));

    assertThat(adapter.embed("Hello world")).containsExactly(0.1f, 0.2f);
  }

  @Test
  @DisplayName("should throw exception when results are empty")
  void shouldThrowExceptionWhenResultsAreEmpty() {
    when(embeddingModel.call(any(EmbeddingRequest.class)))
        .thenReturn(new EmbeddingResponse(List.of()));

    assertThatThrownBy(() -> adapter.embed("Test"))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "RAG_SERVICE_ERROR")
        .hasMessageContaining("Empty embedding response");
  }

  @Test
  @DisplayName("should throw exception when results are null")
  void shouldThrowExceptionWhenResultsAreNull() {
    EmbeddingResponse mockResponse = mock(EmbeddingResponse.class);
    when(mockResponse.getResults()).thenReturn(null);
    when(embeddingModel.call(any(EmbeddingRequest.class))).thenReturn(mockResponse);

    assertThatThrownBy(() -> adapter.embed("Test"))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "RAG_SERVICE_ERROR")
        .hasMessageContaining("Empty embedding response");
  }

  @Test
  @DisplayName("should propagate a domain error unchanged")
  void shouldPropagateADomainErrorUnchanged() {
    when(embeddingModel.call(any(EmbeddingRequest.class)))
        .thenThrow(DomainException.failed("RAG_SERVICE_ERROR", "Service error"));

    assertThatThrownBy(() -> adapter.embed("Test"))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "RAG_SERVICE_ERROR")
        .hasMessage("Service error");
  }

  @Test
  @DisplayName("should wrap a generic exception in a RAG service error")
  void shouldWrapGenericException() {
    when(embeddingModel.call(any(EmbeddingRequest.class)))
        .thenThrow(new RuntimeException("Network error"));

    assertThatThrownBy(() -> adapter.embed("Test"))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "RAG_SERVICE_ERROR")
        .hasMessageContaining("Embedding generation failed")
        .hasCauseInstanceOf(RuntimeException.class);
  }

  @Test
  @DisplayName("should return one embedding per text in input order")
  void shouldReturnOneEmbeddingPerTextInInputOrder() {
    when(embeddingModel.call(any(EmbeddingRequest.class)))
        .thenReturn(
            new EmbeddingResponse(
                List.of(
                    new Embedding(new float[] {0.1f}, 0), new Embedding(new float[] {0.2f}, 1))));

    List<float[]> embeddings = adapter.embedBatch(List.of("Text 1", "Text 2"));

    assertThat(embeddings).containsExactly(new float[] {0.1f}, new float[] {0.2f});
  }

  @Test
  @DisplayName("should throw exception on batch failure")
  void shouldThrowExceptionOnBatchFailure() {
    when(embeddingModel.call(any(EmbeddingRequest.class)))
        .thenThrow(new RuntimeException("Batch error"));

    assertThatThrownBy(() -> adapter.embedBatch(List.of("Text 1", "Text 2")))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "RAG_SERVICE_ERROR")
        .hasMessageContaining("Batch embedding generation failed");
  }

  @Test
  @DisplayName("should return configured dimensions")
  void shouldReturnConfiguredDimensions() {
    assertThat(adapter.getDimensions()).isEqualTo(1024);
  }
}
