package com.ai.domain.model;

import com.ai.domain.vo.DocumentId;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Document entity - represents a document in the RAG system.
 * Rich domain model with business methods for state management.
 */
public class Document {

    private final DocumentId id;
    private String title;
    private String fileName;
    private Long fileSize;
    private DocumentStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private int retryCount;

    public Document(DocumentId id, String title, String fileName, Long fileSize) {
        this.id = Objects.requireNonNull(id, "DocumentId cannot be null");
        this.title = validateTitle(title);
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = DocumentStatus.UPLOADING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.retryCount = 0;
    }

    // Full constructor for repository mapper to restore all fields
    public Document(DocumentId id, String title, String fileName, Long fileSize,
                   DocumentStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "DocumentId cannot be null");
        this.title = validateTitle(title);
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.retryCount = 0;
    }

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            return null; // Allow null title
        }
        if (title.length() > 255) {
            return title.substring(0, 255);
        }
        return title.trim();
    }

    // ============ State Transition Methods ============

    /**
     * Marks document as processing (after upload, before chunks are saved).
     * Can be called from UPLOADING or FAILED states (for retry).
     */
    public void markProcessing() {
        validateTransitionTo(DocumentStatus.PROCESSING);
        this.status = DocumentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks document as ready (all processing completed successfully).
     */
    public void markReady() {
        validateTransitionTo(DocumentStatus.READY);
        this.status = DocumentStatus.READY;
        this.updatedAt = Instant.now();
    }

    /**
     * Marks document as failed.
     */
    public void markFailed() {
        this.status = DocumentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    /**
     * Prepares document for retry after a failure.
     * Only allowed when document is in FAILED status.
     */
    public void prepareForRetry() {
        if (this.status != DocumentStatus.FAILED) {
            throw new IllegalStateException(
                    "Cannot retry document in status: " + this.status);
        }
        this.status = DocumentStatus.UPLOADING;
        this.retryCount++;
        this.updatedAt = Instant.now();
    }

    // ============ Query Methods ============

    public boolean isReady() {
        return this.status == DocumentStatus.READY;
    }

    public boolean isFailed() {
        return this.status == DocumentStatus.FAILED;
    }

    public boolean isProcessing() {
        return this.status == DocumentStatus.PROCESSING;
    }

    public boolean isUploading() {
        return this.status == DocumentStatus.UPLOADING;
    }

    /**
     * Checks if document can transition to the target status.
     */
    public boolean canTransitionTo(DocumentStatus targetStatus) {
        return isValidTransition(this.status, targetStatus);
    }

    /**
     * Returns the number of retry attempts made on this document.
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * Returns the time elapsed since document creation.
     */
    public Duration getProcessingDuration() {
        return Duration.between(createdAt, Instant.now());
    }

    /**
     * Returns the time since last update.
     */
    public Duration getTimeSinceLastUpdate() {
        return Duration.between(updatedAt, Instant.now());
    }

    // ============ Validation ============

    private void validateTransitionTo(DocumentStatus targetStatus) {
        if (!canTransitionTo(targetStatus)) {
            throw new IllegalStateException(
                    "Invalid status transition from " + this.status + " to " + targetStatus);
        }
    }

    private boolean isValidTransition(DocumentStatus from, DocumentStatus to) {
        // Allow flexible transitions for retry and re-processing scenarios
        if (from == to) return false; // No self-transition
        return switch (to) {
            case PROCESSING -> from != DocumentStatus.PROCESSING; // Allow any non-PROCESSING state
            case READY -> from == DocumentStatus.PROCESSING; // Only from PROCESSING
            case FAILED -> from != DocumentStatus.READY; // Allow except from READY (it's terminal)
            case UPLOADING -> false; // Cannot go back to UPLOADING
        };
    }

    // ============ Update Methods ============

    /**
     * Updates the document title.
     */
    public void updateTitle(String newTitle) {
        if (isReady()) {
            throw new IllegalStateException("Cannot update title of ready document");
        }
        this.title = validateTitle(newTitle);
        this.updatedAt = Instant.now();
    }

    // ============ Getters ============

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
