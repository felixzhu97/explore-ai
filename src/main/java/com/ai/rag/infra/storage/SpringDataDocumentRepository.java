package com.ai.rag.infra.storage;

import com.ai.rag.domain.model.DocumentId;
import com.ai.rag.domain.model.RagDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for Document aggregate. */
@Repository
public interface SpringDataDocumentRepository extends JpaRepository<RagDocument, DocumentId> {}
