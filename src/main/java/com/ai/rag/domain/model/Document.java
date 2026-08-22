package com.ai.rag.domain.model;

import com.ai.common.domain.model.AbstractOwnerKeyedEntity;
import com.ai.rag.domain.vo.DocumentId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Document aggregate root with JPA mapping on the documents table. */
@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class Document extends AbstractOwnerKeyedEntity<DocumentId> {

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "file_size")
  private Long fileSize;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private DocumentStatus status;

  /** Documentation. */
  public Document(DocumentId id, String title, String fileName, Long fileSize) {
    this(id, title, fileName, fileSize, com.ai.common.domain.vo.OwnerKey.LEGACY_ORPHAN.value());
  }

  /** Documentation. */
  public Document(DocumentId id, String title, String fileName, Long fileSize, String ownerKey) {
    super(id, ownerKey, Instant.now(), Instant.now());
    this.title = validateTitle(title);
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.status = DocumentStatus.UPLOADING;
  }

  /** Documentation. */
  public Document(
      DocumentId id,
      String title,
      String fileName,
      Long fileSize,
      DocumentStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this(
        id,
        title,
        fileName,
        fileSize,
        status,
        createdAt,
        updatedAt,
        com.ai.common.domain.vo.OwnerKey.LEGACY_ORPHAN.value());
  }

  /** Documentation. */
  public Document(
      DocumentId id,
      String title,
      String fileName,
      Long fileSize,
      DocumentStatus status,
      Instant createdAt,
      Instant updatedAt,
      String ownerKey) {
    super(id, ownerKey, createdAt, updatedAt);
    this.title = validateTitle(title);
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.status = status;
  }

  private static String validateTitle(String title) {
    if (title == null || title.isBlank()) {
      return null;
    }
    return title.length() > 255 ? title.substring(0, 255) : title.trim();
  }

  /** Documentation. */
  public void markProcessing() {
    validateTransitionTo(DocumentStatus.PROCESSING);
    this.status = DocumentStatus.PROCESSING;
    touchUpdatedAt();
  }

  /** Documentation. */
  public void markReady() {
    validateTransitionTo(DocumentStatus.READY);
    this.status = DocumentStatus.READY;
    touchUpdatedAt();
  }

  /** Documentation. */
  public void markFailed() {
    validateTransitionTo(DocumentStatus.FAILED);
    this.status = DocumentStatus.FAILED;
    touchUpdatedAt();
  }

  /** Documentation. */
  public void updateTitle(String newTitle) {
    if (status == DocumentStatus.READY) {
      throw new IllegalStateException("Cannot update title of ready document");
    }
    this.title = validateTitle(newTitle);
    touchUpdatedAt();
  }

  private void validateTransitionTo(DocumentStatus target) {
    if (!isValidTransition(this.status, target)) {
      throw new IllegalStateException(
          "Invalid status transition from " + this.status + " to " + target);
    }
  }

  private boolean isValidTransition(DocumentStatus from, DocumentStatus to) {
    if (from == to) {
      return false;
    }
    return switch (to) {
      case PROCESSING ->
          from == DocumentStatus.UPLOADING
              || from == DocumentStatus.FAILED
              || from == DocumentStatus.READY;
      case READY -> from == DocumentStatus.PROCESSING;
      case FAILED ->
          from == DocumentStatus.UPLOADING
              || from == DocumentStatus.PROCESSING
              || from == DocumentStatus.FAILED;
      case UPLOADING -> false;
    };
  }

  @Override
  public String toString() {
    return "Document{id=%s, title='%s', status=%s}".formatted(getId(), title, status);
  }
}
