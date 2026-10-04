package com.ai.rag.domain.repository;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.vo.DocumentId;
import java.util.List;
import java.util.Map;

/**
 * DocumentChunk repository port - defines the contract for chunk persistence. This interface
 * belongs to the domain layer and is implemented by adapters.
 */
public interface DocumentChunkRepository {

  /** Saves a document chunk. */
  void saveChunk(DocumentChunk chunk);

  /** Finds all chunks belonging to a document. */
  List<DocumentChunk> findChunksByDocumentId(DocumentId documentId);

  /** Counts chunks per document; documents without chunks are absent from the result. */
  Map<DocumentId, Integer> countChunksByDocumentIds(List<DocumentId> documentIds);

  /** Deletes all chunks belonging to a document. */
  void deleteChunksByDocumentId(DocumentId documentId);
}
