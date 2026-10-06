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

  /** Creates a chunk without an embedding. */
  public static DocumentChunk create(
      ChunkId id,
      DocumentId documentId,
      String content,
      int chunkIndex,
      Map<String, Object> metadata) {
    return new DocumentChunk(id, documentId, content, chunkIndex, metadata, null, Instant.now());
  }

  /** Restores a stored chunk. */
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

  /** Returns a copy with the embedding. */
  public DocumentChunk withEmbedding(float[] embedding) {
    return new DocumentChunk(id, documentId, content, chunkIndex, metadata, embedding, createdAt);
  }

  /** Returns the chunk id. */
  public ChunkId getId() {
    return id;
  }

  /** Returns the document id. */
  public DocumentId getDocumentId() {
    return documentId;
  }

  /** Returns the chunk text. */
  public String getContent() {
    return content;
  }

  /** Returns the position of the chunk in the document. */
  public int getChunkIndex() {
    return chunkIndex;
  }

  /** Returns the metadata. */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  /** Returns the embedding. */
  public float[] getEmbedding() {
    return embedding;
  }

  /** Returns when the chunk was created. */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
