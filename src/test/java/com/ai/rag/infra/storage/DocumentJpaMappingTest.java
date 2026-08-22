package com.ai.rag.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.DocumentStatus;
import com.ai.rag.domain.vo.DocumentId;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Document JPA mapping")
class DocumentJpaMappingTest {

  private static final String OWNER_KEY = "c:test-owner";

  @Test
  @DisplayName("should create document aggregate with owner key and domain fields")
  void shouldCreateDocumentAggregateWithOwnerKeyAndDomainFields() {
    DocumentId documentId = DocumentId.of("123e4567-e89b-12d3-a456-426614174000");
    Instant createdAt = Instant.parse("2026-07-01T00:00:00Z");
    Instant updatedAt = Instant.parse("2026-07-02T00:00:00Z");
    Document document =
        new Document(
            documentId,
            "Guide",
            "guide.pdf",
            1024L,
            DocumentStatus.READY,
            createdAt,
            updatedAt,
            OWNER_KEY);

    assertThat(document.getId()).isEqualTo(documentId);
    assertThat(document.getTitle()).isEqualTo("Guide");
    assertThat(document.getFileName()).isEqualTo("guide.pdf");
    assertThat(document.getFileSize()).isEqualTo(1024L);
    assertThat(document.getStatus()).isEqualTo(DocumentStatus.READY);
    assertThat(document.getCreatedAt()).isEqualTo(createdAt);
    assertThat(document.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(document.getClientId()).isEqualTo(OWNER_KEY);
  }
}
