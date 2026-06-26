package com.ai.rag.application.usecase;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.util.VectorSimilarity;
import com.ai.rag.infrastructure.llm.EmbeddingAdapter;
import com.ai.rag.infrastructure.vector.PgVectorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Domain service for RAG retrieval operations.
 * Contains pure retrieval logic without framework dependencies.
 */
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_SOURCE_LENGTH = 500;

    private final EmbeddingAdapter embeddingAdapter;
    private final PgVectorAdapter vectorAdapter;

    public RetrievalService(EmbeddingAdapter embeddingAdapter, PgVectorAdapter vectorAdapter) {
        this.embeddingAdapter = embeddingAdapter;
        this.vectorAdapter = vectorAdapter;
    }

    /**
     * Result of retrieval operation.
     */
    public record RetrievalResult(
        String context,
        List<SourceDocument> sources,
        String enrichedQuery
    ) {}

    /**
     * Retrieves relevant context from vector store for a given query.
     */
    public RetrievalResult retrieve(String query, List<UUID> docIds, int topK) {
        float[] queryEmbedding = embeddingAdapter.embed(query);

        List<DocumentChunk> chunks;
        if (docIds != null && !docIds.isEmpty()) {
            chunks = vectorAdapter.search(queryEmbedding, topK > 0 ? topK : DEFAULT_TOP_K, docIds);
        } else {
            chunks = vectorAdapter.search(queryEmbedding, topK > 0 ? topK : DEFAULT_TOP_K);
        }

        String context = chunks.stream()
            .map(DocumentChunk::getContent)
            .reduce((a, b) -> a + "\n\n" + b)
            .orElse("");

        List<SourceDocument> sources = chunks.stream()
            .map(chunk -> new SourceDocument(
                truncateContent(chunk.getContent()),
                VectorSimilarity.cosineSimilarity(queryEmbedding, chunk.getEmbedding()),
                chunk.getMetadata()
            ))
            .sorted(Comparator.comparingDouble(SourceDocument::score).reversed())
            .toList();

        return new RetrievalResult(context, sources, query);
    }

    private String truncateContent(String content) {
        if (content == null || content.length() <= MAX_SOURCE_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_SOURCE_LENGTH);
    }
}
