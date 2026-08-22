package com.ai.rag.domain.model;

import com.ai.rag.domain.vo.ChunkId;
import com.ai.rag.domain.vo.DocumentId;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * DocumentChunk entity - represents a chunk of a document in the RAG system. Immutable value object
 * with factory method for creation.
 */
public class DocumentChunk {
  private final ChunkId id;
  private final DocumentId documentId;
  private final String content;
  private final int chunkIndex;
  private final Map<String, Object> metadata;
  private final float[] embedding;
  private final Instant createdAt;

  private DocumentChunk(
      ChunkId id,
      DocumentId documentId,
      String content,
      int chunkIndex,
      Map<String, Object> metadata,
      float[] embedding,
      Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id cannot be null");
    this.documentId = Objects.requireNonNull(documentId, "documentId cannot be null");
    this.content = Objects.requireNonNull(content, "content cannot be null");
    this.chunkIndex = chunkIndex;
    this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    this.embedding = embedding;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
  }

  /** Documentation. */
  public static DocumentChunk create(
      DocumentId chunkId,
      DocumentId documentId,
      String content,
      int chunkIndex,
      Map<String, Object> metadata) {
    return create(ChunkId.of(chunkId.value()), documentId, content, chunkIndex, metadata);
  }

  /** Documentation. */
  public static DocumentChunk create(
      ChunkId id,
      DocumentId documentId,
      String content,
      int chunkIndex,
      Map<String, Object> metadata) {
    return new DocumentChunk(id, documentId, content, chunkIndex, metadata, null, Instant.now());
  }

  /** Documentation. */
  public static DocumentChunk reconstitute(
      DocumentId chunkId,
      DocumentId documentId,
      String content,
      int chunkIndex,
      Map<String, Object> metadata,
      float[] embedding,
      Instant createdAt) {
    return reconstitute(
        ChunkId.of(chunkId.value()),
        documentId,
        content,
        chunkIndex,
        metadata,
        embedding,
        createdAt);
  }

  /** Documentation. */
  public static DocumentChunk reconstitute(
      ChunkId id,
      DocumentId documentId,
      String content,
      int chunkIndex,
      Map<String, Object> metadata,
      float[] embedding,
      Instant createdAt) {
    return new DocumentChunk(id, documentId, content, chunkIndex, metadata, embedding, createdAt);
  }

  /** Documentation. */
  public DocumentChunk withEmbedding(float[] embedding) {
    return new DocumentChunk(id, documentId, content, chunkIndex, metadata, embedding, createdAt);
  }

  public ChunkId getId() {
    return id;
  }

  public DocumentId getDocumentId() {
    return documentId;
  }

  public String getContent() {
    return content;
  }

  public int getChunkIndex() {
    return chunkIndex;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public float[] getEmbedding() {
    return embedding;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
