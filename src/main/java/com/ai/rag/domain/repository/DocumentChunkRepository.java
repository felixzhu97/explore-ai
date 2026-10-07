package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.DocumentId;
import java.util.List;

/**
 * DocumentChunk repository port - defines the contract for chunk persistence. This interface
 * belongs to the domain layer and is implemented by adapters.
 */
public interface DocumentChunkRepository {

  /** Finds all chunks belonging to a document. */
  List<DocumentChunk> findChunksByDocumentId(DocumentId documentId);

  /** Saves a document chunk. */
  void saveChunk(DocumentChunk chunk);

  /** Deletes all chunks belonging to a document. */
  void deleteChunksByDocumentId(DocumentId documentId);
}
