package com.ai.application.port;

import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepositoryPort {
    Document save(Document document);
    Optional<Document> findById(UUID id);
    List<Document> findAll();
    void delete(UUID id);
    void flush();

    void saveChunk(DocumentChunk chunk);
    List<DocumentChunk> findChunksByDocumentId(UUID documentId);
    void deleteChunksByDocumentId(UUID documentId);
}
