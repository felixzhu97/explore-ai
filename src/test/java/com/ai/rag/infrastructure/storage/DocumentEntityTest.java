package com.ai.rag.infrastructure.storage;

import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.DocumentStatus;
import com.ai.rag.domain.vo.DocumentId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DocumentEntity")
class DocumentEntityTest {

    @Test
    @DisplayName("should_map_domain_document_to_entity_and_back")
    void should_map_domain_document_to_entity_and_back() {
        UUID uuid = UUID.randomUUID();
        DocumentId documentId = DocumentId.of(uuid);
        Instant createdAt = Instant.parse("2026-07-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-07-02T00:00:00Z");
        Document document = new Document(
                documentId,
                "Guide",
                "guide.pdf",
                1024L,
                DocumentStatus.READY,
                createdAt,
                updatedAt);

        DocumentEntity entity = DocumentEntity.fromDomain(document);
        Document roundTrip = entity.toDomain();

        assertThat(entity.getId()).isEqualTo(uuid);
        assertThat(entity.getTitle()).isEqualTo("Guide");
        assertThat(entity.getFileName()).isEqualTo("guide.pdf");
        assertThat(entity.getFileSize()).isEqualTo(1024L);
        assertThat(entity.getStatus()).isEqualTo(DocumentStatus.READY);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(roundTrip.getId()).isEqualTo(documentId);
        assertThat(roundTrip.getTitle()).isEqualTo("Guide");
        assertThat(roundTrip.getStatus()).isEqualTo(DocumentStatus.READY);
    }

    @Test
    @DisplayName("should_support_bean_setters_and_getters")
    void should_support_bean_setters_and_getters() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        DocumentEntity entity = new DocumentEntity();

        entity.setId(id);
        entity.setTitle("Notes");
        entity.setFileName("notes.txt");
        entity.setFileSize(256L);
        entity.setStatus(DocumentStatus.PROCESSING);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getTitle()).isEqualTo("Notes");
        assertThat(entity.getFileName()).isEqualTo("notes.txt");
        assertThat(entity.getFileSize()).isEqualTo(256L);
        assertThat(entity.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("should_construct_entity_with_all_fields")
    void should_construct_entity_with_all_fields() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-06-02T00:00:00Z");

        DocumentEntity entity = new DocumentEntity(
                id, "Title", "file.md", 512L, DocumentStatus.UPLOADING, createdAt, updatedAt);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getTitle()).isEqualTo("Title");
        assertThat(entity.getStatus()).isEqualTo(DocumentStatus.UPLOADING);
    }
}
