package com.ai.rag.application.usecase;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.util.VectorSimilarity;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.infrastructure.llm.EmbeddingAdapter;
import com.ai.rag.infrastructure.vector.H2VectorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Document retrieval service - handles vector search and context building.
 */
@Service
public class DocumentSearchService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_CONTENT_LENGTH = 500;

    public record RetrievalResult(
            String context,
            List<SourceDocument> sources
    ) {}

    private final EmbeddingAdapter embeddingAdapter;
    private final H2VectorAdapter vectorAdapter;

    public DocumentSearchService(EmbeddingAdapter embeddingAdapter, H2VectorAdapter vectorAdapter) {
        this.embeddingAdapter = embeddingAdapter;
        this.vectorAdapter = vectorAdapter;
    }

    public RetrievalResult retrieve(String query, List<DocumentId> docIds, int topK) {
        log.info("RAG retrieval for query: {}", query);
        float[] queryEmbedding = embeddingAdapter.embed(query);
        int effectiveTopK = topK > 0 ? topK : DEFAULT_TOP_K;

        List<DocumentChunk> chunks;
        if (docIds != null && !docIds.isEmpty()) {
            List<UUID> uuids = docIds.stream().map(DocumentId::value).toList();
            chunks = vectorAdapter.search(queryEmbedding, effectiveTopK, uuids);
        } else {
            chunks = vectorAdapter.search(queryEmbedding, effectiveTopK);
        }

        String context = chunks.stream().map(DocumentChunk::getContent)
                .reduce((a, b) -> a + "\n\n" + b).orElse("");

        List<SourceDocument> sources = chunks.stream()
                .map(chunk -> new SourceDocument(
                        truncate(chunk.getContent()),
                        VectorSimilarity.cosineSimilarity(queryEmbedding, chunk.getEmbedding()),
                        chunk.getMetadata()))
                .sorted(Comparator.comparingDouble(SourceDocument::score).reversed())
                .toList();

        log.info("Retrieved {} chunks", chunks.size());
        return new RetrievalResult(context, sources);
    }

    private String truncate(String content) {
        return content != null && content.length() > MAX_CONTENT_LENGTH
                ? content.substring(0, MAX_CONTENT_LENGTH) + "..."
                : content;
    }
}
