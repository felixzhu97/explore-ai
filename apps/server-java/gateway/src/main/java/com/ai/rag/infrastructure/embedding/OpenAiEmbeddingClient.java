package com.ai.rag.infrastructure.embedding;

import com.ai.rag.domain.EmbeddingClient;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedding client implementation using LangChain4j's EmbeddingModel.
 * Supports DeepSeek and OpenAI-compatible embedding APIs.
 */
@Component
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private final EmbeddingModel embeddingModel;
    private final int dimension;

    public OpenAiEmbeddingClient() {
        this.embeddingModel = null;
        this.dimension = 1536; // Default dimension
    }

    public OpenAiEmbeddingClient(@Autowired EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.dimension = 1536; // text-embedding-3-small uses 1536 dimensions
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[dimension];
        }
        if (embeddingModel == null) {
            return new float[dimension];
        }
        try {
            var embedding = embeddingModel.embed(text);
            var vector = embedding.content().vectorAsList();
            float[] result = new float[vector.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = (float) vector.get(i);
            }
            return result;
        } catch (Exception e) {
            return new float[dimension];
        }
    }

    @Override
    public float[][] embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new float[0][dimension];
        }
        if (embeddingModel == null) {
            float[][] results = new float[texts.size()][dimension];
            return results;
        }
        try {
            // Embed each text individually
            float[][] results = new float[texts.size()][];
            for (int i = 0; i < texts.size(); i++) {
                results[i] = embed(texts.get(i));
            }
            return results;
        } catch (Exception e) {
            float[][] results = new float[texts.size()][dimension];
            return results;
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }
}
