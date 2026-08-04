package com.ai.rag.infrastructure;

import com.ai.rag.domain.Document;
import com.ai.rag.domain.IDocumentRepository;
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
