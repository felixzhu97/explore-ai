package com.ai.rag.infrastructure.vector;

import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.repository.IDocumentChunkRepository;
import com.ai.rag.domain.util.VectorSimilarity;
import com.ai.rag.domain.vo.DocumentId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * H2 vector store adapter.
 * Stores embeddings as JSON arrays and performs cosine-similarity ranking in-process.
 */
@Component
public class H2VectorAdapter implements IDocumentChunkRepository {

    private static final Logger log = LoggerFactory.getLogger(H2VectorAdapter.class);
    private static final String TABLE_NAME = "document_chunks";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final ChunkRowMapper chunkRowMapper;

    public H2VectorAdapter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.chunkRowMapper = new ChunkRowMapper(objectMapper);
    }

    @Override
    @Transactional
    public void saveChunk(DocumentChunk chunk) {
        String embeddingString = arrayToJsonString(chunk.getEmbedding());
        String metadataJson = serializeMetadata(chunk.getMetadata());

        String sql = "MERGE INTO " + TABLE_NAME +
                " (id, document_id, content, chunk_index, embedding, metadata, created_at) " +
                "KEY (id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getContent(),
                chunk.getChunkIndex(),
                embeddingString,
                metadataJson,
                chunk.getCreatedAt().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> findChunksByDocumentId(DocumentId documentId) {
        String sql = "SELECT id, document_id, content, chunk_index, embedding, metadata, created_at " +
                "FROM " + TABLE_NAME + " WHERE document_id = ?";
        return jdbcTemplate.query(sql, chunkRowMapper, documentId.value());
    }

    @Override
    @Transactional
    public void deleteChunksByDocumentId(DocumentId documentId) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE document_id = ?";
        jdbcTemplate.update(sql, documentId.value());
    }

    @Transactional(readOnly = true)
    public List<DocumentChunk> search(float[] queryEmbedding, int topK) {
        return search(queryEmbedding, topK, null);
    }

    @Transactional(readOnly = true)
    public List<DocumentChunk> search(float[] queryEmbedding, int topK, List<UUID> docIds) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return List.of();
        }
        List<DocumentChunk> candidates = loadCandidates(docIds);

        return candidates.stream()
                .filter(chunk -> chunk.getEmbedding() != null && chunk.getEmbedding().length == queryEmbedding.length)
                .sorted(Comparator.comparingDouble((DocumentChunk chunk) ->
                        VectorSimilarity.cosineSimilarity(queryEmbedding, chunk.getEmbedding()))
                        .reversed())
                .limit(topK)
                .toList();
    }

    private List<DocumentChunk> loadCandidates(List<UUID> docIds) {
        if (docIds != null && !docIds.isEmpty()) {
            String placeholders = docIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String sql = "SELECT id, document_id, content, chunk_index, embedding, metadata, created_at " +
                    "FROM " + TABLE_NAME + " WHERE document_id IN (" + placeholders + ")";
            return jdbcTemplate.query(sql, chunkRowMapper, docIds.toArray());
        }

        String sql = "SELECT id, document_id, content, chunk_index, embedding, metadata, created_at " +
                "FROM " + TABLE_NAME;
        return jdbcTemplate.query(sql, chunkRowMapper);
    }

    private String arrayToJsonString(float[] array) {
        if (array == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize metadata", e);
            return null;
        }
    }
}
