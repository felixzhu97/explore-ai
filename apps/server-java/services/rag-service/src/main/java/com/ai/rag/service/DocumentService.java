package com.ai.rag.service;

import com.ai.rag.exception.RagException;
import com.ai.rag.model.Document;
import com.ai.rag.repository.DocumentRepository;
import com.ai.rag.repository.DocumentRepository.DocumentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service for document upload, processing, and management.
 * Handles file ingestion, text chunking, and vector storage.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final VectorSearchService vectorSearchService;
    private final ChunkingService chunkingService;

    @Value("${rag.chunk-size:500}")
    private int defaultChunkSize;

    @Value("${rag.chunk-overlap:50}")
    private int defaultChunkOverlap;

    public DocumentService(
            DocumentRepository documentRepository,
            VectorSearchService vectorSearchService,
            ChunkingService chunkingService
    ) {
        this.documentRepository = documentRepository;
        this.vectorSearchService = vectorSearchService;
        this.chunkingService = chunkingService;
    }

    /**
     * Upload and ingest a document.
     *
     * @param file Multipart file to upload
     * @return Uploaded Document
     */
    public Document upload(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new RagException("Filename must not be blank");
        }

        String contentType = file.getContentType();
        Long size = file.getSize();

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RagException("Failed to read uploaded file", e);
        }

        return ingestContent(filename, contentType, size, content);
    }

    /**
     * Ingest raw text content as a document.
     *
     * @param title  Document title
     * @param content Text content
     * @return Created Document
     */
    public Document ingestText(String title, String content) {
        return ingestContent(title, "text/plain", (long) content.length(), content);
    }

    /**
     * Internal method to ingest content.
     */
    private Document ingestContent(String filename, String contentType, Long size, String content) {
        // Update status to indexing
        Document doc = documentRepository.save(filename, contentType, size, List.of());

        try {
            documentRepository.updateStatus(doc.id(), DocumentStatus.INDEXING);

            // Chunk the text
            List<String> chunks = chunkingService.chunkText(content, defaultChunkSize, defaultChunkOverlap);

            if (chunks.isEmpty()) {
                throw new RagException("No content extracted from document");
            }

            // Add chunks to vector store with metadata
            vectorSearchService.addSegments(chunks, doc.id().toString(), filename);

            // Update document with chunk count
            documentRepository.updateChunkCount(doc.id(), chunks.size());
            documentRepository.updateStatus(doc.id(), DocumentStatus.COMPLETED);

            log.info("Document ingested: {} with {} chunks", doc.id(), chunks.size());

            return documentRepository.findById(doc.id());

        } catch (Exception e) {
            log.error("Failed to ingest document: {}", filename, e);
            documentRepository.updateStatus(doc.id(), DocumentStatus.FAILED);
            throw new RagException("Failed to ingest document: " + e.getMessage(), e);
        }
    }

    /**
     * Get all documents with pagination.
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return List of documents
     */
    public List<Document> findAll(int page, int size) {
        return documentRepository.findAll(page, size);
    }

    /**
     * Get all documents.
     *
     * @return List of all documents
     */
    public List<Document> findAll() {
        return documentRepository.findAll(0, 1000);
    }

    /**
     * Find document by ID.
     *
     * @param id Document ID
     * @return Document or null
     */
    public Document findById(UUID id) {
        return documentRepository.findById(id);
    }

    /**
     * Delete document and its vectors.
     *
     * @param id Document ID
     */
    public void delete(UUID id) {
        // Delete from vector store
        vectorSearchService.deleteByDocId(id.toString());

        // Delete from repository
        documentRepository.deleteById(id);

        log.info("Deleted document: {}", id);
    }

    /**
     * Get document statistics.
     *
     * @param id Document ID
     * @return Statistics map
     */
    public Map<String, Object> getStats(UUID id) {
        Document doc = documentRepository.findById(id);
        if (doc == null) {
            throw new RagException("Document not found: " + id);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("doc_id", doc.id().toString());
        stats.put("filename", doc.filename());
        stats.put("content_type", doc.contentType());
        stats.put("size", doc.size());
        stats.put("chunk_count", doc.chunkCount());
        stats.put("status", documentRepository.getStatus(id));
        stats.put("created_at", doc.createdAt().toString());

        return stats;
    }

    /**
     * Get all document IDs.
     *
     * @return Set of document IDs
     */
    public Set<UUID> findAllIds() {
        return documentRepository.findAllIds();
    }

    /**
     * Get total document count.
     *
     * @return Document count
     */
    public long count() {
        return documentRepository.count();
    }
}
