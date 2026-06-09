package com.ai.rag.infrastructure.rag;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for RAG service.
 * Supports both Qdrant (production) and in-memory (development) modes.
 */
@ConfigurationProperties("rag")
public record RagProperties(
        @DefaultValue QdrantConfig qdrant,
        @DefaultValue ChunkingConfig chunking
) {

    @ConfigurationProperties("qdrant")
    public record QdrantConfig(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("localhost") String host,
            @DefaultValue("6333") int port,
            @DefaultValue("ai_test_docs") String collection,
            @DefaultValue("1536") int vectorDimension,
            @DefaultValue("30000") long timeout,
            @DefaultValue("false") boolean useTls
    ) {
        public String getCollection() {
            return collection;
        }
    }

    @ConfigurationProperties("chunking")
    public record ChunkingConfig(
            @DefaultValue("500") int chunkSize,
            @DefaultValue("50") int chunkOverlap
    ) {
        public int resolvedChunkSize() {
            return chunkSize > 0 ? chunkSize : 500;
        }

        public int resolvedChunkOverlap() {
            return chunkOverlap >= 0 ? chunkOverlap : 50;
        }
    }

    public QdrantConfig getQdrant() {
        return qdrant != null ? qdrant : new QdrantConfig(true, "localhost", 6333, "ai_test_docs", 1536, 30000, false);
    }

    public ChunkingConfig getChunking() {
        return chunking != null ? chunking : new ChunkingConfig(500, 50);
    }
}
