package com.ai.rag.domain;

import java.util.List;

/**
 * Repository interface for vector store operations.
 * Defined in Domain layer (ports), implemented in Infrastructure layer.
 */
public interface VectorStore {

    /**
     * Adds text chunks to the vector store.
     */
    void addSegments(List<String> chunks, DocumentId docId, String filename);

    /**
     * Searches for similar text chunks.
     */
    List<String> searchSimilar(String query, int topK);

    /**
     * Searches for similar text chunks with document filtering.
     */
    List<String> searchSimilar(String query, List<String> docIds, int topK);

    /**
     * Searches and returns results with scores.
     */
    List<SourceDocument> searchWithScores(String query, int topK);

    /**
     * Searches and returns results with scores, filtered by document IDs.
     */
    List<SourceDocument> searchWithScores(String query, List<String> docIds, int topK);

    /**
     * Deletes all vectors for a document.
     */
    void deleteByDocId(DocumentId docId);

    /**
     * Gets statistics from the vector store.
     */
    java.util.Map<String, Object> getStats();
}
