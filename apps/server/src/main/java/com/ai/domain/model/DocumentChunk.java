package com.ai.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DocumentChunk {
    private final UUID id;
    private final UUID documentId;
    private final String content;
    private final int chunkIndex;
    private final Map<String, Object> metadata;
    private final float[] embedding;
    private final Instant createdAt;

    public DocumentChunk(UUID id, UUID documentId, String content, int chunkIndex, Map<String, Object> metadata) {
        this.id = id;
        this.documentId = documentId;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.metadata = metadata;
        this.embedding = null;
        this.createdAt = Instant.now();
    }

    DocumentChunk(UUID id, UUID documentId, String content, int chunkIndex, Map<String, Object> metadata, float[] embedding, Instant createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.metadata = metadata;
        this.embedding = embedding;
        this.createdAt = createdAt;
    }

    public DocumentChunk withEmbedding(float[] embedding) {
        return new DocumentChunk(this.id, this.documentId, this.content, this.chunkIndex, this.metadata, embedding, this.createdAt);
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public String getContent() { return content; }
    public int getChunkIndex() { return chunkIndex; }
    public Map<String, Object> getMetadata() { return metadata; }
    public float[] getEmbedding() { return embedding; }
    public Instant getCreatedAt() { return createdAt; }
}
