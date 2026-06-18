package com.ai.infrastructure.adapter.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "rag.mock.embeddings", havingValue = "false", matchIfMissing = true)
public class OllamaEmbeddingAdapter implements EmbeddingAdapter {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingAdapter.class);

    private final EmbeddingModel embeddingModel;
    private final int dimensions;

    public OllamaEmbeddingAdapter(
            EmbeddingModel embeddingModel,
            @Value("${rag.ollama.embedding.dimensions:768}") int dimensions) {
        this.embeddingModel = embeddingModel;
        this.dimensions = dimensions;
    }

    public float[] embed(String text) {
        log.debug("Generating Ollama embedding for text (length={})", text.length());
        
        try {
            EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
            EmbeddingResponse response = embeddingModel.call(request);
            
            if (response.getResults() == null || response.getResults().isEmpty()) {
                throw new RuntimeException("Empty embedding response from Ollama API");
            }
            
            float[] result = response.getResults().get(0).getOutput();
            log.debug("Generated Ollama embedding with {} dimensions", result.length);
            return result;
            
        } catch (Exception e) {
            log.error("Failed to generate embedding for text", e);
            throw new RuntimeException("Embedding generation failed: " + e.getMessage(), e);
        }
    }

    public List<float[]> embedBatch(List<String> texts) {
        log.debug("Generating Ollama embeddings for {} texts", texts.size());
        
        try {
            EmbeddingRequest request = new EmbeddingRequest(texts, null);
            EmbeddingResponse response = embeddingModel.call(request);
            
            return response.getResults().stream()
                    .map(embeddingResult -> embeddingResult.getOutput())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Failed to generate embeddings for batch", e);
            throw new RuntimeException("Batch embedding generation failed: " + e.getMessage(), e);
        }
    }

    public int getDimensions() {
        return dimensions;
    }
}
