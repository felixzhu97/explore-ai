package com.ai.rag.domain.model;

import com.ai.common.domain.model.AbstractOwnerAwareEntity;
import com.ai.common.domain.model.DomainStrings;
import com.ai.common.domain.model.OwnerKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Uploaded RAG document aggregate; chunks and embeddings hang off it. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class RagDocument extends AbstractOwnerAwareEntity<DocumentId> {

  public static final String UNTITLED = "Untitled";
  static final int MAX_TITLE_LENGTH = 255;
  private static final String TITLE_METADATA_KEY = "title";
  private static final String FILE_NAME_METADATA_KEY = "fileName";

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
      String ownerKey) {
    super(id, OwnerKey.parseKey(ownerKey));
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
    return new RagDocument(
        DocumentId.generateId(), title, fileName, fileSize, DocumentStatus.PROCESSING, 0, ownerKey);
  }

  private static String resolveTitle(String title, String fileName) {
    String resolved =
        title != null && !title.isBlank()
            ? title.trim()
            : fileName != null && !fileName.isBlank() ? fileName.trim() : UNTITLED;
    return DomainStrings.truncate(resolved, MAX_TITLE_LENGTH);
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
  }

  /** Marks ingestion failed; safe to call again so the original error stays visible. */
  public void failIngestion() {
    if (status == DocumentStatus.FAILED) {
      return;
    }
    this.status = DocumentStatus.FAILED;
    this.chunkCount = 0;
  }

  /** Tells whether the document can be searched and offered to the model. */
  public boolean isSearchable() {
    return status == DocumentStatus.READY;
  }

  /** Creates the chunk at {@code index}, tagged with this document's owner, title and file. */
  public DocumentChunk createChunk(int index, String content, Map<String, Object> sourceMetadata) {
    Map<String, Object> metadata = new HashMap<>(sourceMetadata);
    metadata.put(TITLE_METADATA_KEY, title);
    if (fileName != null) {
      metadata.put(FILE_NAME_METADATA_KEY, fileName);
    }
    return DocumentChunk.createChunk(
        ChunkId.generateId(), getId(), ownerKey, content, index, metadata);
  }

  @Override
  public String toString() {
    return "RagDocument{id=%s, status=%s, chunkCount=%d}".formatted(getId(), status, chunkCount);
  }
}
