package com.ai.adapter.out.persistence;

import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.DocumentStatus;
import com.ai.domain.repository.DocumentRepository;
import com.ai.domain.vo.DocumentId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Document repository using Spring Data JPA.
 */
@Component
public class JpaDocumentRepository implements DocumentRepository {

    private final SpringDataDocumentRepository documentRepository;
    private final SpringDataChunkRepository chunkRepository;
    private final ObjectMapper objectMapper;

    public JpaDocumentRepository(SpringDataDocumentRepository documentRepository,
                                  SpringDataChunkRepository chunkRepository,
                                  ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Document save(Document document) {
        DocumentEntity entity = toEntity(document);
        DocumentEntity saved = documentRepository.save(entity);
        return toDomain(saved);
    }

    @Transactional(readOnly = true)
    public Optional<Document> findById(UUID id) {
        return documentRepository.findById(id).map(this::toDomain);
    }

    @Transactional(readOnly = true)
    public List<Document> findAll() {
        return documentRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(UUID id) {
        chunkRepository.deleteByDocumentId(id);
        documentRepository.deleteById(id);
    }

    @Transactional
    public void saveChunk(DocumentChunk chunk) {
        DocumentChunkEntity entity = toChunkEntity(chunk);
        chunkRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<DocumentChunk> findChunksByDocumentId(UUID documentId) {
        return chunkRepository.findByDocumentId(documentId).stream()
                .map(this::toChunkDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteChunksByDocumentId(UUID documentId) {
        chunkRepository.deleteByDocumentId(documentId);
    }

    private DocumentEntity toEntity(Document document) {
        DocumentEntity.DocumentStatus status;
        switch (document.getStatus()) {
            case UPLOADING -> status = DocumentEntity.DocumentStatus.UPLOADING;
            case PROCESSING -> status = DocumentEntity.DocumentStatus.PROCESSING;
            case READY -> status = DocumentEntity.DocumentStatus.READY;
            case FAILED -> status = DocumentEntity.DocumentStatus.FAILED;
            default -> throw new IllegalArgumentException("Unknown status: " + document.getStatus());
        }

        return new DocumentEntity(
                document.getId().value(),
                document.getTitle(),
                document.getFileName(),
                document.getFileSize(),
                status,
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private Document toDomain(DocumentEntity entity) {
        DocumentStatus status;
        switch (entity.getStatus()) {
            case UPLOADING -> status = DocumentStatus.UPLOADING;
            case PROCESSING -> status = DocumentStatus.PROCESSING;
            case READY -> status = DocumentStatus.READY;
            case FAILED -> status = DocumentStatus.FAILED;
            default -> throw new IllegalArgumentException("Unknown status: " + entity.getStatus());
        }

        return new Document(
                DocumentId.of(entity.getId()),
                entity.getTitle(),
                entity.getFileName(),
                entity.getFileSize(),
                status,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private DocumentChunkEntity toChunkEntity(DocumentChunk chunk) {
        Float[] embeddingArray = null;
        if (chunk.getEmbedding() != null) {
            embeddingArray = new Float[chunk.getEmbedding().length];
            for (int i = 0; i < chunk.getEmbedding().length; i++) {
                embeddingArray[i] = chunk.getEmbedding()[i];
            }
        }

        String metadataJson = null;
        if (chunk.getMetadata() != null && !chunk.getMetadata().isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(chunk.getMetadata());
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize chunk metadata", e);
            }
        }

        return new DocumentChunkEntity(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getContent(),
                chunk.getChunkIndex(),
                embeddingArray,
                metadataJson,
                chunk.getCreatedAt()
        );
    }

    private DocumentChunk toChunkDomain(DocumentChunkEntity entity) {
        float[] embedding = null;
        if (entity.getEmbedding() != null) {
            embedding = new float[entity.getEmbedding().length];
            for (int i = 0; i < entity.getEmbedding().length; i++) {
                embedding[i] = entity.getEmbedding()[i];
            }
        }

        Map<String, Object> metadata = new HashMap<>();
        if (entity.getMetadata() != null && !entity.getMetadata().isEmpty()) {
            try {
                metadata = objectMapper.readValue(entity.getMetadata(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException ignored) {
            }
        }

        return new DocumentChunk(
                entity.getId(),
                entity.getDocumentId(),
                entity.getContent(),
                entity.getChunkIndex(),
                metadata
        ).withEmbedding(embedding);
    }
}
