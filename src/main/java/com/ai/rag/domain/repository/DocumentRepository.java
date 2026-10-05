package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.RagDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Document repository port - defines the contract for document persistence. This interface belongs
 * to the domain layer and is implemented by adapters.
 */
public interface DocumentRepository {

  /** Finds a document by its ID. */
  Optional<RagDocument> findById(UUID id);

  /** Retrieves all documents. */
  List<RagDocument> findAll();

  List<RagDocument> findAllByOwnerKey(String ownerKey);

  Optional<RagDocument> findByIdAndOwnerKey(java.util.UUID id, String ownerKey);

  /** Saves a document and returns the saved entity. */
  RagDocument save(RagDocument document);

  void deleteByIdAndOwnerKey(java.util.UUID id, String ownerKey);

  /** Deletes a document by its ID. */
  void delete(UUID id);
}
