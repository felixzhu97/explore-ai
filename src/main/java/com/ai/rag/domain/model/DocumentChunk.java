package com.ai.rag.domain.model;

import com.ai.common.domain.model.OwnerKey;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;

/** Immutable chunk of an uploaded document, with its embedding once one is computed. */
@Getter
public class DocumentChunk {

  /** Longest excerpt shown to the model or the user for one chunk. */
  public static final int EXCERPT_LENGTH = 500;

  private static final String OWNER_KEY_METADATA_KEY = "ownerKey";

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
  public static DocumentChunk createChunk(
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
  public static DocumentChunk restoreChunk(
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
    copy.remove(OWNER_KEY_METADATA_KEY);
    return Map.copyOf(copy);
  }

  /** Returns a copy with the embedding. */
  public DocumentChunk copyWithEmbedding(float[] embedding) {
    return new DocumentChunk(
        id, documentId, ownerKey, content, chunkIndex, metadata, embedding, createdAt);
  }

  /** Cosine similarity to the query embedding; 0 when either side is missing or sizes differ. */
  public double calculateSimilarity(float[] queryEmbedding) {
    if (!isComparableWith(queryEmbedding)) {
      return 0.0;
    }
    double dotProduct = 0.0;
    double queryNorm = 0.0;
    double chunkNorm = 0.0;
    for (int i = 0; i < embedding.length; i++) {
      dotProduct += queryEmbedding[i] * embedding[i];
      queryNorm += queryEmbedding[i] * queryEmbedding[i];
      chunkNorm += embedding[i] * embedding[i];
    }
    double denominator = Math.sqrt(queryNorm) * Math.sqrt(chunkNorm);
    return denominator > 0 ? dotProduct / denominator : 0.0;
  }

  /** Tells whether the chunk has an embedding comparable with the query embedding. */
  public boolean isComparableWith(float[] queryEmbedding) {
    return embedding != null && queryEmbedding != null && embedding.length == queryEmbedding.length;
  }

  /** Returns the chunk text cut to {@value #EXCERPT_LENGTH} characters plus an ellipsis. */
  public String getExcerpt() {
    return content.length() <= EXCERPT_LENGTH
        ? content
        : content.substring(0, EXCERPT_LENGTH) + "...";
  }

  /** Returns a copy of the embedding, or null when it has none. */
  public float[] getEmbedding() {
    return embedding != null ? embedding.clone() : null;
  }
}
