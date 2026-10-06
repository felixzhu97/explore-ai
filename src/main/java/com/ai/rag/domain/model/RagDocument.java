package com.ai.rag.domain.model;

import com.ai.common.domain.model.AbstractOwnerKeyedEntity;
import com.ai.rag.domain.vo.ChunkId;
import com.ai.rag.domain.vo.DocumentId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Uploaded RAG document aggregate; chunks and embeddings hang off it. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class RagDocument extends AbstractOwnerKeyedEntity<DocumentId> {

  public static final String UNTITLED = "Untitled";
  static final int MAX_TITLE_LENGTH = 255;

  @Column(nullable = false)
  private String title;

  @Column private String fileName;

  @Column private Long fileSize;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DocumentStatus status;

  @Column(nullable = false)
  private int chunkCount;

  private RagDocument(
      DocumentId id,
      String title,
      String fileName,
      Long fileSize,
      DocumentStatus status,
      int chunkCount,
      Instant createdAt,
      Instant updatedAt,
      String ownerKey) {
    super(id, ownerKey, createdAt, updatedAt);
    this.title = resolveTitle(title, fileName);
    this.fileName = fileName;
    this.fileSize = fileSize;
    this.status = status;
    this.chunkCount = chunkCount;
  }

  /**
   * Starts ingesting an uploaded file. The title falls back to the file name, then to {@value
   * #UNTITLED}; empty files are rejected.
   */
  public static RagDocument startIngestion(
      String title, String fileName, long fileSize, String ownerKey) {
    if (fileSize <= 0) {
      throw new IllegalArgumentException("Uploaded file is empty");
    }
    Instant now = Instant.now();
    return new RagDocument(
        DocumentId.generate(),
        title,
        fileName,
        fileSize,
        DocumentStatus.PROCESSING,
        0,
        now,
        now,
        ownerKey);
  }

  /** Restores a stored document. */
  public static RagDocument restore(
      DocumentId id,
      String title,
      String fileName,
      Long fileSize,
      DocumentStatus status,
      int chunkCount,
      Instant createdAt,
      Instant updatedAt,
      String ownerKey) {
    return new RagDocument(
        id, title, fileName, fileSize, status, chunkCount, createdAt, updatedAt, ownerKey);
  }

  private static String resolveTitle(String title, String fileName) {
    String resolved =
        title != null && !title.isBlank()
            ? title.trim()
            : fileName != null && !fileName.isBlank() ? fileName.trim() : UNTITLED;
    return resolved.length() > MAX_TITLE_LENGTH
        ? resolved.substring(0, MAX_TITLE_LENGTH)
        : resolved;
  }

  /** Marks ingestion done with the number of chunks stored. */
  public void completeIngestion(int storedChunks) {
    if (status != DocumentStatus.PROCESSING) {
      throw new IllegalStateException("Only a processing document can complete ingestion");
    }
    if (storedChunks <= 0) {
      throw new IllegalArgumentException("A ready document needs at least one chunk");
    }
    this.chunkCount = storedChunks;
    this.status = DocumentStatus.READY;
    touchUpdatedAt();
  }

  /** Marks ingestion failed; safe to call again so the original error stays visible. */
  public void failIngestion() {
    if (status == DocumentStatus.FAILED) {
      return;
    }
    this.status = DocumentStatus.FAILED;
    this.chunkCount = 0;
    touchUpdatedAt();
  }

  /** Tells whether the document can be searched and offered to the model. */
  public boolean isSearchable() {
    return status == DocumentStatus.READY;
  }

  /** Creates the chunk at {@code index}, tagged with this document's owner, title and file. */
  public DocumentChunk newChunk(int index, String content, Map<String, Object> sourceMetadata) {
    Map<String, Object> metadata = new HashMap<>(sourceMetadata);
    metadata.put(ChunkMetadataKeys.TITLE, title);
    if (fileName != null) {
      metadata.put(ChunkMetadataKeys.FILE_NAME, fileName);
    }
    return DocumentChunk.create(ChunkId.generate(), getId(), ownerKey, content, index, metadata);
  }

  /** Changes the title unless the document is already READY. */
  public void updateTitle(String newTitle) {
    if (status == DocumentStatus.READY) {
      throw new IllegalStateException("Cannot update title of ready document");
    }
    this.title = resolveTitle(newTitle, fileName);
    touchUpdatedAt();
  }

  @Override
  public String toString() {
    return "RagDocument{id=%s, status=%s, chunkCount=%d}".formatted(getId(), status, chunkCount);
  }
}
