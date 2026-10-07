package com.ai.rag.domain.repository;

import com.ai.common.domain.model.OwnerKey;
import com.ai.rag.domain.model.DocumentId;
import com.ai.rag.domain.model.RagDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository of uploaded documents, partitioned by owner. */
public interface DocumentRepository extends Repository<RagDocument, DocumentId> {

  /** Finds the owner's document by id. */
  Optional<RagDocument> findByIdAndOwnerKey(DocumentId id, OwnerKey ownerKey);

  /** Lists the owner's documents, newest first. */
  List<RagDocument> findAllByOwnerKeyOrderByCreatedAtDesc(OwnerKey ownerKey);

  /** Saves the document and returns the stored copy. */
  RagDocument save(RagDocument document);

  /** Deletes the owner's document by id. */
  @Transactional
  void deleteByIdAndOwnerKey(DocumentId id, OwnerKey ownerKey);
}
