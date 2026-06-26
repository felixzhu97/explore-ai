package com.ai.rag.domain.model;

import com.ai.rag.domain.vo.DocumentId;

import java.time.Instant;
import java.util.Objects;

/**
 * Document entity - rich domain model with state management.
 */
public class Document {

    private final DocumentId id;
    private String title;
    private String fileName;
    private Long fileSize;
    private DocumentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Document(DocumentId id, String title, String fileName, Long fileSize) {
        this.id = Objects.requireNonNull(id, "DocumentId cannot be null");
        this.title = validateTitle(title);
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = DocumentStatus.UPLOADING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Document(DocumentId id, String title, String fileName, Long fileSize,
                   DocumentStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "DocumentId cannot be null");
        this.title = validateTitle(title);
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) return null;
        return title.length() > 255 ? title.substring(0, 255) : title.trim();
    }

    public void markProcessing() {
        validateTransitionTo(DocumentStatus.PROCESSING);
        this.status = DocumentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markReady() {
        validateTransitionTo(DocumentStatus.READY);
        this.status = DocumentStatus.READY;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        validateTransitionTo(DocumentStatus.FAILED);
        this.status = DocumentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void updateTitle(String newTitle) {
        if (status == DocumentStatus.READY) {
            throw new IllegalStateException("Cannot update title of ready document");
        }
        this.title = validateTitle(newTitle);
        this.updatedAt = Instant.now();
    }

    private void validateTransitionTo(DocumentStatus target) {
        if (!isValidTransition(this.status, target)) {
            throw new IllegalStateException(
                    "Invalid status transition from " + this.status + " to " + target);
        }
    }

    private boolean isValidTransition(DocumentStatus from, DocumentStatus to) {
        if (from == to) return false;
        return switch (to) {
            case PROCESSING -> from == DocumentStatus.UPLOADING
                    || from == DocumentStatus.FAILED
                    || from == DocumentStatus.READY;
            case READY -> from == DocumentStatus.PROCESSING;
            case FAILED -> from == DocumentStatus.UPLOADING
                    || from == DocumentStatus.PROCESSING
                    || from == DocumentStatus.FAILED;
            case UPLOADING -> false;
        };
    }

    public DocumentId getId() { return id; }
    public String getTitle() { return title; }
    public String getFileName() { return fileName; }
    public Long getFileSize() { return fileSize; }
    public DocumentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Document{id=%s, title='%s', status=%s}".formatted(id, title, status);
    }
}
