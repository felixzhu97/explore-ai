package com.ai.rag.infra.storage;

import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.vo.DocumentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for Document aggregate. */
@Repository
public interface SpringDataDocumentRepository extends JpaRepository<Document, DocumentId> {}
