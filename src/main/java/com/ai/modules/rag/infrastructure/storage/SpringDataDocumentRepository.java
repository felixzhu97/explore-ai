package com.ai.modules.rag.infrastructure.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for DocumentEntity.
 * Located in infrastructure layer to avoid domain-dependency violation.
 */
@Repository
public interface SpringDataDocumentRepository extends JpaRepository<DocumentEntity, UUID> {
}
