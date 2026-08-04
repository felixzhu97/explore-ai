package com.ai.rag.domain;

import com.ai.rag.domain.DocumentChunk;
import com.ai.rag.domain.DocumentId;

import java.util.List;

/**
 * DocumentChunk repository port - defines the contract for chunk persistence.
 * This interface belongs to the domain layer and is implemented by adapters.
 */
public interface IDocumentChunkRepository {

    /**
     * Saves a document chunk.
     */
    void saveChunk(DocumentChunk chunk);

    /**
     * Finds all chunks belonging to a document.
     */
    List<DocumentChunk> findChunksByDocumentId(DocumentId documentId);

    /**
     * Deletes all chunks belonging to a document.
     */
    void deleteChunksByDocumentId(DocumentId documentId);
}
