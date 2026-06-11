package com.ai.application.usecase;

import com.ai.application.port.EmbeddingPort;
import com.ai.application.port.VectorSearchPort;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.SourceDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RagChatUseCase {
    private static final Logger log = LoggerFactory.getLogger(RagChatUseCase.class);
    private static final int DEFAULT_TOP_K = 5;

    private final EmbeddingPort embeddingPort;
    private final VectorSearchPort vectorSearchPort;

    public RagChatUseCase(EmbeddingPort embeddingPort, VectorSearchPort vectorSearchPort) {
        this.embeddingPort = embeddingPort;
        this.vectorSearchPort = vectorSearchPort;
    }

    public record RetrievalResult(String context, List<SourceDocument> sources) {}

    public RetrievalResult execute(String query, List<UUID> docIds, int topK) {
        log.info("RAG retrieval for query: {}", query);
        
        float[] queryEmbedding = embeddingPort.embed(query);
        
        List<DocumentChunk> chunks;
        if (docIds != null && !docIds.isEmpty()) {
            chunks = vectorSearchPort.search(queryEmbedding, topK > 0 ? topK : DEFAULT_TOP_K, docIds);
        } else {
            chunks = vectorSearchPort.search(queryEmbedding, topK > 0 ? topK : DEFAULT_TOP_K);
        }

        String context = chunks.stream()
            .map(DocumentChunk::getContent)
            .collect(Collectors.joining("\n\n"));

        List<SourceDocument> sources = chunks.stream()
            .map(chunk -> new SourceDocument(
                chunk.getContent().substring(0, Math.min(500, chunk.getContent().length())),
                calculateSimilarity(queryEmbedding, chunk.getEmbedding()),
                chunk.getMetadata()
            ))
            .sorted(Comparator.comparingDouble(SourceDocument::score).reversed())
            .toList();

        log.info("Retrieved {} chunks", chunks.size());
        return new RetrievalResult(context, sources);
    }

    private double calculateSimilarity(float[] a, float[] b) {
        if (a == null || b == null) return 0.0;
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
    }
}
