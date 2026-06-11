package com.ai.infrastructure.adapter.persistence;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
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
 * JPA implementation of DocumentRepositoryPort.
 * Uses Spring Data JPA repositories for persistence.
 */
@Component
public class JpaDocumentRepository implements DocumentRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(JpaDocumentRepository.class);

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

    @Override
    @Transactional
    public Document save(Document document) {
        log.debug("Saving document: id={}, title={}", document.getId(), document.getTitle());
        
        DocumentEntity entity = toEntity(document);
        DocumentEntity saved = documentRepository.save(entity);
        
        log.info("Document saved successfully: id={}", saved.getId());
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findById(UUID id) {
        log.debug("Finding document by id: {}", id);
        return documentRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findAll() {
        log.debug("Finding all documents");
        return documentRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.debug("Deleting document: id={}", id);
        chunkRepository.deleteByDocumentId(id);
        documentRepository.deleteById(id);
        log.info("Document deleted successfully: id={}", id);
    }

    @Override
    @Transactional
    public void saveChunk(DocumentChunk chunk) {
        log.debug("Saving chunk: id={}, documentId={}, chunkIndex={}", 
                  chunk.getId(), chunk.getDocumentId(), chunk.getChunkIndex());
        
        DocumentChunkEntity entity = toEntity(chunk);
        chunkRepository.save(entity);
        
        log.info("Chunk saved successfully: id={}", chunk.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> findChunksByDocumentId(UUID documentId) {
        log.debug("Finding chunks by documentId: {}", documentId);
        return chunkRepository.findByDocumentId(documentId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteChunksByDocumentId(UUID documentId) {
        log.debug("Deleting chunks by documentId: {}", documentId);
        chunkRepository.deleteByDocumentId(documentId);
        log.info("Chunks deleted successfully for documentId: {}", documentId);
    }

    // Mapping methods

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
                document.getId(),
                document.getTitle(),
                document.getFileName(),
                document.getFileSize(),
                status,
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private Document toDomain(DocumentEntity entity) {
        Document.DocumentStatus status;
        switch (entity.getStatus()) {
            case UPLOADING -> status = Document.DocumentStatus.UPLOADING;
            case PROCESSING -> status = Document.DocumentStatus.PROCESSING;
            case READY -> status = Document.DocumentStatus.READY;
            case FAILED -> status = Document.DocumentStatus.FAILED;
            default -> throw new IllegalArgumentException("Unknown status: " + entity.getStatus());
        }

        return new Document(
                entity.getId(),
                entity.getTitle(),
                entity.getFileName(),
                entity.getFileSize(),
                status,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private DocumentChunkEntity toEntity(DocumentChunk chunk) {
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
                log.error("Failed to serialize chunk metadata", e);
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

    private DocumentChunk toDomain(DocumentChunkEntity entity) {
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
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize chunk metadata", e);
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
