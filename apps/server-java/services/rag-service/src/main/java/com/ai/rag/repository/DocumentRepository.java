package com.ai.rag.repository;

import com.ai.rag.exception.RagException;
import com.ai.rag.model.Document;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory document repository for storing document metadata.
 * Provides CRUD operations for document management.
 */
@Repository
public class DocumentRepository {

    private final Map<UUID, DocumentRecord> documents = new ConcurrentHashMap<>();

    /**
     * Save a new document.
     *
     * @param filename   Original filename
     * @param contentType MIME type
     * @param size       File size in bytes
     * @param chunks     List of text chunks
     * @return Saved Document
     */
    public Document save(String filename, String contentType, Long size, List<String> chunks) {
        UUID documentId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Document document = new Document(
                documentId,
                filename,
                contentType,
                size,
                chunks != null ? chunks.size() : 0,
                now
        );

        documents.put(documentId, new DocumentRecord(document, DocumentStatus.PENDING));

        return document;
    }

    /**
     * Save a document with explicit ID.
     *
     * @param docId      Document ID
     * @param filename   Original filename
     * @param contentType MIME type
     * @param size       File size in bytes
     * @param chunkCount Number of chunks
     * @return Saved Document
     */
    public Document save(UUID docId, String filename, String contentType, Long size, int chunkCount) {
        LocalDateTime now = LocalDateTime.now();

        Document document = new Document(
                docId,
                filename,
                contentType,
                size,
                chunkCount,
                now
        );

        documents.put(docId, new DocumentRecord(document, DocumentStatus.PENDING));

        return document;
    }

    /**
     * Find all documents with pagination.
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of documents
     */
    public List<Document> findAll(int page, int size) {
        return documents.values().stream()
                .sorted((a, b) -> b.document.createdAt().compareTo(a.document.createdAt()))
                .skip((long) page * size)
                .limit(size)
                .map(record -> record.document)
                .collect(Collectors.toList());
    }

    /**
     * Find all documents with a specific status.
     *
     * @param status Document status to filter
     * @param page   Page number
     * @param size   Page size
     * @return List of matching documents
     */
    public List<Document> findByStatus(DocumentStatus status, int page, int size) {
        return documents.values().stream()
                .filter(record -> record.status == status)
                .sorted((a, b) -> b.document.createdAt().compareTo(a.document.createdAt()))
                .skip((long) page * size)
                .limit(size)
                .map(record -> record.document)
                .collect(Collectors.toList());
    }

    /**
     * Find document by ID.
     *
     * @param id Document ID
     * @return Document or null if not found
     */
    public Document findById(UUID id) {
        DocumentRecord record = documents.get(id);
        return record != null ? record.document : null;
    }

    /**
     * Check if document exists.
     *
     * @param id Document ID
     * @return true if exists
     */
    public boolean existsById(UUID id) {
        return documents.containsKey(id);
    }

    /**
     * Update document status.
     *
     * @param id     Document ID
     * @param status New status
     */
    public void updateStatus(UUID id, DocumentStatus status) {
        DocumentRecord record = documents.get(id);
        if (record != null) {
            documents.put(id, new DocumentRecord(record.document, status));
        }
    }

    /**
     * Update document with new information.
     *
     * @param doc Document to update
     */
    public void update(Document doc) {
        DocumentRecord record = documents.get(doc.id());
        if (record != null) {
            documents.put(doc.id(), new DocumentRecord(doc, record.status));
        } else {
            documents.put(doc.id(), new DocumentRecord(doc, DocumentStatus.PENDING));
        }
    }

    /**
     * Update document chunk count.
     *
     * @param id         Document ID
     * @param chunkCount New chunk count
     */
    public void updateChunkCount(UUID id, int chunkCount) {
        DocumentRecord record = documents.get(id);
        if (record != null) {
            Document updated = new Document(
                    record.document.id(),
                    record.document.filename(),
                    record.document.contentType(),
                    record.document.size(),
                    chunkCount,
                    record.document.createdAt()
            );
            documents.put(id, new DocumentRecord(updated, record.status));
        }
    }

    /**
     * Delete document by ID.
     *
     * @param id Document ID
     */
    public void deleteById(UUID id) {
        documents.remove(id);
    }

    /**
     * Count total documents.
     *
     * @return Total document count
     */
    public long count() {
        return documents.size();
    }

    /**
     * Count documents by status.
     *
     * @param status Status to count
     * @return Count of documents with status
     */
    public long countByStatus(DocumentStatus status) {
        return documents.values().stream()
                .filter(record -> record.status == status)
                .count();
    }

    /**
     * Get document status.
     *
     * @param id Document ID
     * @return Document status or null
     */
    public DocumentStatus getStatus(UUID id) {
        DocumentRecord record = documents.get(id);
        return record != null ? record.status : null;
    }

    /**
     * Clear all documents.
     */
    public void deleteAll() {
        documents.clear();
    }

    /**
     * Get all document IDs.
     *
     * @return Set of all document IDs
     */
    public Set<UUID> findAllIds() {
        return new HashSet<>(documents.keySet());
    }

    /**
     * Internal record for storing document with status.
     */
    private record DocumentRecord(Document document, DocumentStatus status) {}

    /**
     * Document processing status.
     */
    public enum DocumentStatus {
        PENDING,
        INDEXING,
        COMPLETED,
        FAILED
    }
}
