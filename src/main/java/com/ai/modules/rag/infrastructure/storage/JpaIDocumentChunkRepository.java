package com.ai.modules.rag.infrastructure.storage;

import com.ai.modules.rag.domain.model.DocumentChunk;
import com.ai.modules.rag.domain.repository.IDocumentChunkRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DocumentChunk repository using Spring Data JPA.
 */
@Component
public class JpaIDocumentChunkRepository implements IDocumentChunkRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaIDocumentChunkRepository.class);

    private final SpringDataChunkRepository chunkRepository;
    private final ObjectMapper objectMapper;

    public JpaIDocumentChunkRepository(SpringDataChunkRepository chunkRepository,
                                       ObjectMapper objectMapper) {
        this.chunkRepository = chunkRepository;
        this.objectMapper = objectMapper;
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
                log.error("Failed to serialize chunk metadata", e);
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
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse metadata JSON: {}", e.getMessage());
            }
        }

        return DocumentChunk.reconstitute(
                entity.getId(),
                entity.getDocumentId(),
                entity.getContent(),
                entity.getChunkIndex(),
                metadata,
                embedding,
                entity.getCreatedAt()
        ).withEmbedding(embedding);
    }
}
