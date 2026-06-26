package com.ai.rag.infrastructure.storage;

import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.repository.IDocumentRepository;
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
}
