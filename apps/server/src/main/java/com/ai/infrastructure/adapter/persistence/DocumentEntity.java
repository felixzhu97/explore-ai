package com.ai.infrastructure.adapter.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for Document aggregate.
 * Maps to the documents table in PostgreSQL.
 */
@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DocumentEntity() {
    }

    public DocumentEntity(UUID id, String title, String fileName, Long fileSize, 
                          DocumentStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public enum DocumentStatus {
        UPLOADING, PROCESSING, READY, FAILED
    }

    public static DocumentEntity fromDomain(com.ai.domain.model.Document document) {
        return new DocumentEntity(
            document.getId().value(),
            document.getTitle(),
            document.getFileName(),
            document.getFileSize(),
            DocumentStatus.valueOf(document.getStatus().name()),
            document.getCreatedAt(),
            document.getUpdatedAt()
        );
    }

    public com.ai.domain.model.Document toDomain() {
        return new com.ai.domain.model.Document(
            com.ai.domain.vo.DocumentId.of(id),
            title,
            fileName,
            fileSize
        );
    }
}
