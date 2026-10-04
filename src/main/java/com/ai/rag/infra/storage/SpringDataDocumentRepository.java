package com.ai.rag.infra.storage;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.vo.DocumentId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for Document aggregate. */
@Repository
public interface SpringDataDocumentRepository extends JpaRepository<Document, DocumentId> {

  /** Documentation. */
  List<Document> findAllByOwnerKeyOrderByCreatedAtDesc(OwnerKey ownerKey);

  /** Documentation. */
  Optional<Document> findByIdAndOwnerKey(DocumentId id, OwnerKey ownerKey);

  /** Documentation. */
  void deleteByIdAndOwnerKey(DocumentId id, OwnerKey ownerKey);
}
