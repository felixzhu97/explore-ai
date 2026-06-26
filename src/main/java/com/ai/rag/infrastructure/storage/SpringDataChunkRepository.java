package com.ai.rag.infrastructure.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for DocumentChunkEntity.
 * Located in infrastructure layer to avoid domain-dependency violation.
 */
@Repository
public interface SpringDataChunkRepository extends JpaRepository<DocumentChunkEntity, UUID> {

    List<DocumentChunkEntity> findByDocumentId(UUID documentId);

    @Modifying
    @Query("DELETE FROM DocumentChunkEntity c WHERE c.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);
}
