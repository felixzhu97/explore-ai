package com.ai.rag.domain.model;

import com.ai.common.domain.model.OwnerKey;
import com.ai.rag.domain.service.VectorSimilarity;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * DocumentChunk entity - represents a chunk of a document in the RAG system. Immutable value object
 * with factory method for creation.
 */
public class DocumentChunk {

  /** Longest excerpt shown to the model or the user for one chunk. */
  public static final int EXCERPT_LENGTH = 500;

  private final ChunkId id;
  private final DocumentId documentId;
  private final OwnerKey ownerKey;
  private final String content;
  private final int chunkIndex;
  private final Map<String, Object> metadata;
  private final float[] embedding;
  private final Instant createdAt;

  private DocumentChunk(
      ChunkId id,
      DocumentId documentId,
      OwnerKey ownerKey,
      String content,
      int chunkIndex,
      Map<String, Object> metadata,
      float[] embedding,
      Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id cannot be null");
    this.documentId = Objects.requireNonNull(documentId, "documentId cannot be null");
    this.ownerKey = Objects.requireNonNull(ownerKey, "ownerKey cannot be null");
    this.content = Objects.requireNonNull(content, "content cannot be null");
    this.chunkIndex = chunkIndex;
    this.metadata = withoutOwner(metadata);
    this.embedding = embedding != null ? embedding.clone() : null;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
  }

  /** Creates a chunk without an embedding. */
  public static DocumentChunk create(
      ChunkId id,
      DocumentId documentId,
      OwnerKey ownerKey,
      String content,
      int chunkIndex,
      Map<String, Object> metadata) {
    return new DocumentChunk(
        id, documentId, ownerKey, content, chunkIndex, metadata, null, Instant.now());
  }

  /** Restores a stored chunk. */
  public static DocumentChunk reconstitute(
      ChunkId id,
      DocumentId documentId,
      OwnerKey ownerKey,
      String content,
      int chunkIndex,
      Map<String, Object> metadata,
      float[] embedding,
      Instant createdAt) {
    return new DocumentChunk(
        id, documentId, ownerKey, content, chunkIndex, metadata, embedding, createdAt);
  }

  private static Map<String, Object> withoutOwner(Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> copy = new HashMap<>(metadata);
    copy.remove(ChunkMetadataKeys.OWNER_KEY);
    return Map.copyOf(copy);
  }

  /** Returns a copy with the embedding. */
  public DocumentChunk withEmbedding(float[] embedding) {
    return new DocumentChunk(
        id, documentId, ownerKey, content, chunkIndex, metadata, embedding, createdAt);
  }

  /** Cosine similarity to the query embedding; 0 when either side is missing or sizes differ. */
  public double similarityTo(float[] queryEmbedding) {
    return VectorSimilarity.calculateCosineSimilarity(queryEmbedding, embedding);
  }

  /** Tells whether the chunk has an embedding comparable with the query embedding. */
  public boolean isComparableWith(float[] queryEmbedding) {
    return embedding != null && queryEmbedding != null && embedding.length == queryEmbedding.length;
  }

  /** Returns the chunk text cut to {@value #EXCERPT_LENGTH} characters plus an ellipsis. */
  public String excerpt() {
    return content.length() <= EXCERPT_LENGTH
        ? content
        : content.substring(0, EXCERPT_LENGTH) + "...";
  }

  /** Returns the chunk id. */
  public ChunkId getId() {
    return id;
  }

  /** Returns the document id. */
  public DocumentId getDocumentId() {
    return documentId;
  }

  /** Returns the owner of the document the chunk came from. */
  public OwnerKey getOwnerKey() {
    return ownerKey;
  }

  /** Returns the chunk text. */
  public String getContent() {
    return content;
  }

  /** Returns the position of the chunk in the document. */
  public int getChunkIndex() {
    return chunkIndex;
  }

  /** Returns the metadata, without the owner. */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  /** Returns a copy of the embedding, or null when it has none. */
  public float[] getEmbedding() {
    return embedding != null ? embedding.clone() : null;
  }

  /** Returns when the chunk was created. */
  public Instant getCreatedAt() {
    return createdAt;
  }
}
