package com.ai.domain.repository;

import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Document repository port - defines the contract for document persistence.
 * This interface belongs to the domain layer and is implemented by adapters.
 */
public interface DocumentRepository {

    /**
     * Saves a document and returns the saved entity.
     */
    Document save(Document document);

    /**
     * Finds a document by its ID.
     */
    Optional<Document> findById(UUID id);

    /**
     * Retrieves all documents.
     */
    List<Document> findAll();

    /**
     * Deletes a document by its ID.
     */
    void delete(UUID id);

    /**
     * Saves a document chunk.
     */
    void saveChunk(DocumentChunk chunk);

    /**
     * Finds all chunks belonging to a document.
     */
    List<DocumentChunk> findChunksByDocumentId(UUID documentId);

    /**
     * Deletes all chunks belonging to a document.
     */
    void deleteChunksByDocumentId(UUID documentId);
}
