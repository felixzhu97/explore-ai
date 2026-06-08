package com.ai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("rag")
public record RagProperties(
        Qdrant qdrant,
        Llm llm
) {
    public record Qdrant(
            String host,
            Integer port,
            String collectionName,
            String embeddingModelName,
            Integer chunkSize,
            Integer chunkOverlap
    ) {
    }

    public record Llm(
            String provider,
            String modelName,
            String apiKey
    ) {
    }
}
