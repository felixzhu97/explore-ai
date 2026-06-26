package com.ai.rag.infrastructure.storage;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.repository.IDocumentChunkRepository;
import com.ai.rag.domain.vo.DocumentId;
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
    public List<DocumentChunk> findChunksByDocumentId(DocumentId documentId) {
        return chunkRepository.findByDocumentId(documentId.value()).stream()
                .map(this::toChunkDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteChunksByDocumentId(DocumentId documentId) {
        chunkRepository.deleteByDocumentId(documentId.value());
    }

    private static float[] toPrimitive(Float[] array) {
        float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    private static Float[] toObject(float[] array) {
        Float[] result = new Float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    private DocumentChunkEntity toChunkEntity(DocumentChunk chunk) {
        Float[] embeddingArray = chunk.getEmbedding() != null ? toObject(chunk.getEmbedding()) : null;

        String metadataJson = null;
        if (chunk.getMetadata() != null && !chunk.getMetadata().isEmpty()) {
            try {
                metadataJson = objectMapper.writeValueAsString(chunk.getMetadata());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize chunk metadata", e);
            }
        }

        return new DocumentChunkEntity(
                chunk.getId().value(),
                chunk.getDocumentId().value(),
                chunk.getContent(),
                chunk.getChunkIndex(),
                embeddingArray,
                metadataJson,
                chunk.getCreatedAt()
        );
    }

    private DocumentChunk toChunkDomain(DocumentChunkEntity entity) {
        float[] embedding = entity.getEmbedding() != null ? toPrimitive(entity.getEmbedding()) : null;

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
                DocumentId.of(entity.getId()),
                DocumentId.of(entity.getDocumentId()),
                entity.getContent(),
                entity.getChunkIndex(),
                metadata,
                embedding,
                entity.getCreatedAt()
        );
    }
}
