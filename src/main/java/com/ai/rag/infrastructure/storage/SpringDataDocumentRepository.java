package com.ai.rag.infrastructure.storage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for DocumentEntity. */
@Repository
public interface SpringDataDocumentRepository extends JpaRepository<DocumentEntity, UUID> {
  /** Documentation. */
  List<DocumentEntity> findByOwnerKeyOrderByCreatedAtDesc(String ownerKey);

  /** Documentation. */
  Optional<DocumentEntity> findByIdAndOwnerKey(UUID id, String ownerKey);

  /** Documentation. */
  void deleteByIdAndOwnerKey(UUID id, String ownerKey);
}
