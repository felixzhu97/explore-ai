package com.ai.common.domain.port.out;

import java.util.List;

/**
 * Port interface for document search capabilities.
 * Implemented by RAG module adapters to provide document retrieval.
 */
public interface DocumentSearchTool {

    /**
     * Search uploaded Documents and return Source Documents.
     *
     * @param query the search query
     * @param docIds optional list of document IDs to filter
     * @return formatted search results
     */
    String searchDocuments(String query, List<String> docIds);

    /**
     * List all uploaded Documents.
     *
     * @return formatted list of documents
     */
    String listDocuments();
}
