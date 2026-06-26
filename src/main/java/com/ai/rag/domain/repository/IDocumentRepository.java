package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.Document;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Document repository port - defines the contract for document persistence.
 * This interface belongs to the domain layer and is implemented by adapters.
 */
public interface IDocumentRepository {

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
}
