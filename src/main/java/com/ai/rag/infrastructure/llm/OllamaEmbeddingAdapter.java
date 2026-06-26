package com.ai.rag.infrastructure.llm;

import com.ai.rag.domain.exception.RagServiceException;
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

    private final EmbeddingModel embeddingModel;
    private final int dimensions;

    public OllamaEmbeddingAdapter(
            EmbeddingModel embeddingModel,
            @Value("${rag.ollama.embedding.dimensions:768}") int dimensions) {
        this.embeddingModel = embeddingModel;
        this.dimensions = dimensions;
    }

    public float[] embed(String text) {
        try {
            EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
            EmbeddingResponse response = embeddingModel.call(request);
            
            if (response.getResults() == null || response.getResults().isEmpty()) {
                throw new RagServiceException("Empty embedding response from Ollama API");
            }
            
            return response.getResults().get(0).getOutput();
            
        } catch (RagServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new RagServiceException("Embedding generation failed", e);
        }
    }

    public List<float[]> embedBatch(List<String> texts) {
        try {
            EmbeddingRequest request = new EmbeddingRequest(texts, null);
            EmbeddingResponse response = embeddingModel.call(request);
            
            return response.getResults().stream()
                    .map(embeddingResult -> embeddingResult.getOutput())
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            throw new RagServiceException("Batch embedding generation failed", e);
        }
    }

    public int getDimensions() {
        return dimensions;
    }
}
