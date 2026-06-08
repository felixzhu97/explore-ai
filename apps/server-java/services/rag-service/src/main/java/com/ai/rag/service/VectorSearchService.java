package com.ai.rag.service;

import com.ai.rag.store.QdrantEmbeddingStore;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for vector similarity search operations.
 * Uses Qdrant as the underlying vector store via custom EmbeddingStore implementation.
 */
@Service
public class VectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

    private final EmbeddingModel embeddingModel;
    private final QdrantEmbeddingStore embeddingStore;

    /**
     * Constructor for Spring dependency injection.
     */
    public VectorSearchService(
            EmbeddingModel embeddingModel,
            QdrantEmbeddingStore embeddingStore
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        log.info("VectorSearchService initialized with QdrantEmbeddingStore");
    }

    /**
     * Search for similar text chunks based on a query string.
     *
     * @param query The search query
     * @param topK  Number of results to return
     * @return List of matching text chunks
     */
    public List<String> searchSimilar(String query, int topK) {
        Embedding queryEmbedding = embeddingModel.embed(query);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        return result.matches().stream()
                .map(match -> match.embedded().text())
                .toList();
    }

    /**
     * Search for similar text chunks with filtering by document IDs.
     *
     * @param query  The search query
     * @param docIds Optional list of document IDs to filter results
     * @param topK   Number of results to return
     * @return List of matching text chunks
     */
    public List<String> searchSimilar(String query, List<String> docIds, int topK) {
        Embedding queryEmbedding = embeddingModel.embed(query);

        if (docIds == null || docIds.isEmpty()) {
            return searchSimilar(query, topK);
        }

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        return result.matches().stream()
                .filter(match -> {
                    if (match.embedded() == null || match.embedded().metadata() == null) {
                        return true;
                    }
                    String matchDocId = match.embedded().metadata().retrieve("doc_id");
                    return docIds.contains(matchDocId);
                })
                .map(match -> match.embedded().text())
                .toList();
    }

    /**
     * Search and return matches with scores for ranking/filtering.
     *
     * @param query The search query
     * @param topK  Number of results to return
     * @return List of embedding matches with scores
     */
    public List<EmbeddingMatch<TextSegment>> searchWithScores(String query, int topK) {
        Embedding queryEmbedding = embeddingModel.embed(query);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .build();

        return embeddingStore.search(request).matches();
    }

    /**
     * Add text chunks to the vector store.
     *
     * @param chunks List of text chunks to add
     */
    public void addSegments(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<TextSegment> segments = chunks.stream()
                .map(TextSegment::from)
                .toList();

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        embeddingStore.addAll(embeddings, segments);

        log.info("Added {} segments to vector store", chunks.size());
    }

    /**
     * Add text chunks with metadata to the vector store.
     *
     * @param chunks   List of text chunks to add
     * @param docId    Document ID for all chunks
     * @param filename Original filename
     */
    public void addSegments(List<String> chunks, String docId, String filename) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<TextSegment> segments = chunks.stream()
                .map(chunk -> TextSegment.from(chunk, new dev.langchain4j.data.document.Metadata(
                        java.util.Map.of(
                                "doc_id", docId,
                                "filename", filename != null ? filename : ""
                        )
                )))
                .toList();

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        embeddingStore.addAll(embeddings, segments);

        log.info("Added {} segments for doc {} to vector store", chunks.size(), docId);
    }

    /**
     * Delete all vectors associated with a document.
     *
     * @param docId Document ID
     */
    public void deleteByDocId(String docId) {
        embeddingStore.deleteByDocId(docId);
        log.info("Deleted vectors for doc_id: {}", docId);
    }

    /**
     * Get statistics about the vector store.
     *
     * @return Statistics map
     */
    public java.util.Map<String, Object> getStats() {
        return embeddingStore.getStats();
    }
}
