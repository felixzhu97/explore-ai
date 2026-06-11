package com.ai.infrastructure.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for DocumentEntity.
 */
@Repository
public interface SpringDataDocumentRepository extends JpaRepository<DocumentEntity, UUID> {
}
