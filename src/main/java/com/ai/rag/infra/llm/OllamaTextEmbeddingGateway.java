package com.ai.rag.infra.llm;

import com.ai.common.exception.DomainException;
import com.ai.rag.domain.repository.TextEmbeddingGateway;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Spring AI embedding adapter. Works with Ollama (local) or OpenAI (cloud) EmbeddingModel beans.
 */
@Component
@ConditionalOnBean(EmbeddingModel.class)
public class OllamaTextEmbeddingGateway implements TextEmbeddingGateway {

  private final EmbeddingModel embeddingModel;
  private final int dimensions;

  public OllamaTextEmbeddingGateway(
      EmbeddingModel embeddingModel,
      @Value("${app.rag.embedding.dimensions:${spring.ai.ollama.embedding.dimensions:1024}}")
          int dimensions) {
    this.embeddingModel = embeddingModel;
    this.dimensions = dimensions;
  }

  @Override
  public float[] embedText(String text) {
    try {
      if (text == null || text.isBlank()) {
        return new float[dimensions];
      }

      EmbeddingResponse response =
          embeddingModel.call(
              new org.springframework.ai.embedding.EmbeddingRequest(List.of(text), null));

      // Extract embedding from response
      List<org.springframework.ai.embedding.Embedding> embeddings = response.getResults();
      if (embeddings == null || embeddings.isEmpty()) {
        throw DomainException.failed(
            "RAG_SERVICE_ERROR", "Empty embedding response from Ollama API");
      }

      // Get the embedding output (may be float[] or List<Float>)
      Object embeddingOutput = embeddings.get(0).getOutput();
      float[] result = convertToFloatArray(embeddingOutput);

      return result;

    } catch (DomainException e) {
      throw e;
    } catch (Exception e) {
      throw DomainException.failed(
          "RAG_SERVICE_ERROR", "Embedding generation failed: " + e.getMessage(), e);
    }
  }

  @Override
  public List<float[]> embedBatch(List<String> texts) {
    try {
      EmbeddingResponse response =
          embeddingModel.call(new org.springframework.ai.embedding.EmbeddingRequest(texts, null));

      List<org.springframework.ai.embedding.Embedding> embeddings = response.getResults();
      return embeddings.stream().map(e -> convertToFloatArray(e.getOutput())).toList();

    } catch (Exception e) {
      throw DomainException.failed(
          "RAG_SERVICE_ERROR", "Batch embedding generation failed: " + e.getMessage(), e);
    }
  }

  @Override
  public int getDimensions() {
    return dimensions;
  }

  private float[] convertToFloatArray(Object output) {
    if (output instanceof float[] arr) {
      return arr;
    } else if (output instanceof List<?> list) {
      float[] result = new float[list.size()];
      for (int i = 0; i < list.size(); i++) {
        Object item = list.get(i);
        if (item instanceof Number num) {
          result[i] = num.floatValue();
        }
      }
      return result;
    } else if (output instanceof double[] arr) {
      float[] result = new float[arr.length];
      for (int i = 0; i < arr.length; i++) {
        result[i] = (float) arr[i];
      }
      return result;
    }
    throw DomainException.failed(
        "RAG_SERVICE_ERROR", "Unsupported embedding output type: " + output.getClass());
  }
}
