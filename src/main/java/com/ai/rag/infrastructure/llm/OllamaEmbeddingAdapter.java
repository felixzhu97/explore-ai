package com.ai.rag.infrastructure.llm;

import com.ai.rag.domain.exception.RagServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ollama embedding adapter using Spring AI.
 * Generates embeddings using local Ollama models (e.g., nomic-embed-text).
 */
@Component
@ConditionalOnProperty(name = "spring.ai.ollama.embedding.enabled", havingValue = "true", matchIfMissing = true)
public class OllamaEmbeddingAdapter implements EmbeddingAdapter {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingAdapter.class);

    private final EmbeddingModel embeddingModel;
    private final int dimensions;

    public OllamaEmbeddingAdapter(
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.ollama.embedding.dimensions:768}") int dimensions) {
        this.embeddingModel = embeddingModel;
        this.dimensions = dimensions;
    }

    @Override
    public float[] embed(String text) {
        try {
            if (text == null || text.isBlank()) {
                log.warn("Empty text received for embedding, returning zero vector");
                return new float[dimensions];
            }

            log.debug("Generating embedding for text of length: {}", text.length());
            EmbeddingResponse response = embeddingModel.call(
                    new org.springframework.ai.embedding.EmbeddingRequest(List.of(text), null));

            // Extract embedding from response
            List<org.springframework.ai.embedding.Embedding> embeddings = response.getResults();
            if (embeddings == null || embeddings.isEmpty()) {
                throw new RagServiceException("Empty embedding response from Ollama API");
            }

            // Get the embedding output (may be float[] or List<Float>)
            Object embeddingOutput = embeddings.get(0).getOutput();
            float[] result = convertToFloatArray(embeddingOutput);

            log.debug("Generated embedding with {} dimensions", result.length);
            return result;

        } catch (RagServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Embedding failed: {}", e.getMessage(), e);
            throw new RagServiceException("Embedding generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        try {
            log.debug("Generating batch embeddings for {} texts", texts.size());
            EmbeddingResponse response = embeddingModel.call(
                    new org.springframework.ai.embedding.EmbeddingRequest(texts, null));

            List<org.springframework.ai.embedding.Embedding> embeddings = response.getResults();
            return embeddings.stream()
                    .map(e -> convertToFloatArray(e.getOutput()))
                    .toList();

        } catch (Exception e) {
            log.error("Batch embedding failed: {}", e.getMessage(), e);
            throw new RagServiceException("Batch embedding generation failed: " + e.getMessage(), e);
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
        throw new RagServiceException("Unsupported embedding output type: " + output.getClass());
    }
}
