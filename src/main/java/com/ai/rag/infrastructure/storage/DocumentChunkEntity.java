package com.ai.rag.infrastructure.storage;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for DocumentChunk.
 * Maps to the document_chunks table in PostgreSQL with pgvector extension.
 */
@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_document_chunks_document_id", columnList = "document_id")
})
public class DocumentChunkEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "embedding", columnDefinition = "vector")
    private Float[] embedding;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public DocumentChunkEntity() {
    }

    public DocumentChunkEntity(UUID id, UUID documentId, String content, int chunkIndex,
                               Float[] embedding, String metadata, Instant createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.embedding = embedding;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public Float[] getEmbedding() { return embedding; }
    public void setEmbedding(Float[] embedding) { this.embedding = embedding; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
