package com.ai.modules.rag.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * DocumentChunk entity - represents a chunk of a document in the RAG system.
 * Immutable value object with factory method for creation.
 */
public class DocumentChunk {
    private final UUID id;
    private final UUID documentId;
    private final String content;
    private final int chunkIndex;
    private final Map<String, Object> metadata;
    private final float[] embedding;
    private final Instant createdAt;

    private DocumentChunk(UUID id, UUID documentId, String content, int chunkIndex,
                         Map<String, Object> metadata, float[] embedding, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.documentId = Objects.requireNonNull(documentId, "documentId cannot be null");
        this.content = Objects.requireNonNull(content, "content cannot be null");
        this.chunkIndex = chunkIndex;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.embedding = embedding;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    /**
     * Creates a new DocumentChunk with the given parameters.
     * Embedding will be set later via withEmbedding().
     */
    public static DocumentChunk create(UUID id, UUID documentId, String content,
                                       int chunkIndex, Map<String, Object> metadata) {
        return new DocumentChunk(id, documentId, content, chunkIndex, metadata, null, Instant.now());
    }

    /**
     * Creates a new DocumentChunk with the given parameters and embedding.
     */
    public static DocumentChunk createWithEmbedding(UUID id, UUID documentId, String content,
                                                    int chunkIndex, Map<String, Object> metadata,
                                                    float[] embedding) {
        return new DocumentChunk(id, documentId, content, chunkIndex, metadata, embedding, Instant.now());
    }

    /**
     * Factory method to create a DocumentChunk from persistence storage.
     * Used by repository mappers in the infrastructure layer.
     */
    public static DocumentChunk reconstitute(UUID id, UUID documentId, String content,
                                     int chunkIndex, Map<String, Object> metadata,
                                     float[] embedding, Instant createdAt) {
        return new DocumentChunk(id, documentId, content, chunkIndex, metadata, embedding, createdAt);
    }

    /**
     * Creates a copy of this chunk with the given embedding.
     * Since DocumentChunk is immutable, this returns a new instance.
     */
    public DocumentChunk withEmbedding(float[] embedding) {
        return new DocumentChunk(id, documentId, content, chunkIndex, metadata, embedding, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public String getContent() { return content; }
    public int getChunkIndex() { return chunkIndex; }
    public Map<String, Object> getMetadata() { return metadata; }
    public float[] getEmbedding() { return embedding; }
    public Instant getCreatedAt() { return createdAt; }
}
