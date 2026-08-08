package com.ai.rag.infrastructure.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for DocumentEntity.
 */
@Repository
public interface SpringDataDocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    List<DocumentEntity> findByOwnerKeyOrderByCreatedAtDesc(String ownerKey);

    Optional<DocumentEntity> findByIdAndOwnerKey(UUID id, String ownerKey);

    void deleteByIdAndOwnerKey(UUID id, String ownerKey);
}
