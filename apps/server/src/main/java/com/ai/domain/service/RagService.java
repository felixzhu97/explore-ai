package com.ai.domain.service;

import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.repository.DocumentRepository;
import com.ai.domain.vo.DocumentId;
import com.ai.adapter.out.embedding.EmbeddingAdapter;
import com.ai.adapter.out.vector.PgVectorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_SOURCE_LENGTH = 500;

    private final ChunkingService chunkingService;
    private final EmbeddingAdapter embeddingAdapter;
    private final PgVectorAdapter vectorAdapter;
    private final DocumentRepository documentRepository;

    public RagService(
            ChunkingService chunkingService,
            EmbeddingAdapter embeddingAdapter,
            PgVectorAdapter vectorAdapter,
            DocumentRepository documentRepository) {
        this.chunkingService = chunkingService;
        this.embeddingAdapter = embeddingAdapter;
        this.vectorAdapter = vectorAdapter;
        this.documentRepository = documentRepository;
    }

    public record RetrievalResult(
        String context,
        List<SourceDocument> sources,
        String enrichedQuery
    ) {}

    @Transactional
    public Document uploadDocument(String title, String fileName, Long fileSize, String content) {
        log.info("Uploading document: {}", title);

        Document document = new Document(DocumentId.generate(), title, fileName, fileSize);
        document.markProcessing();
        document = saveDocument(document);

        try {
            List<String> chunks = chunkingService.chunk(content);
            log.info("Split into {} chunks", chunks.size());

            List<DocumentChunk> embeddedChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                float[] embedding = embeddingAdapter.embed(chunkText);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("title", title);
                metadata.put("fileName", fileName);

                DocumentChunk chunk = new DocumentChunk(
                    UUID.randomUUID(),
                    document.getId().value(),
                    chunkText,
                    i,
                    metadata
                ).withEmbedding(embedding);

                try {
                    vectorAdapter.saveChunk(chunk);
                    embeddedChunks.add(chunk);
                    log.debug("Chunk {}/{} saved successfully for document {}", i + 1, chunks.size(), document.getId());
                } catch (Exception e) {
                    log.error("Failed to save chunk {} for document {}: {}", i, document.getId(), e.getMessage());
                    throw e;
                }
            }

            log.info("All {} chunks saved to vector store for document {}", embeddedChunks.size(), document.getId());

            document.markReady();
            document = saveDocument(document);
            log.info("Document uploaded successfully: {}", document.getId());
            return document;

        } catch (Exception e) {
            log.error("Failed to process document", e);
            document.markFailed();
            saveDocument(document);
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }

    public List<Document> listDocuments() {
        return documentRepository.findAll();
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        log.info("Deleting document: {}", documentId);

        documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        vectorAdapter.search(new float[embeddingAdapter.getDimensions()], 1000, List.of(documentId));
        
        documentRepository.delete(documentId);

        log.info("Document deleted successfully: {}", documentId);
    }

    public RetrievalResult retrieveContext(String query, List<UUID> docIds, int topK) {
        log.info("RAG retrieval for query: {}", query);

        float[] queryEmbedding = embeddingAdapter.embed(query);

        List<DocumentChunk> chunks;
        if (docIds != null && !docIds.isEmpty()) {
            chunks = vectorAdapter.search(queryEmbedding, topK > 0 ? topK : DEFAULT_TOP_K, docIds);
        } else {
            chunks = vectorAdapter.search(queryEmbedding, topK > 0 ? topK : DEFAULT_TOP_K);
        }

        String context = chunks.stream()
            .map(DocumentChunk::getContent)
            .collect(Collectors.joining("\n\n"));

        List<SourceDocument> sources = chunks.stream()
            .map(chunk -> new SourceDocument(
                chunk.getContent().substring(0, Math.min(MAX_SOURCE_LENGTH, chunk.getContent().length())),
                calculateSimilarity(queryEmbedding, chunk.getEmbedding()),
                chunk.getMetadata()
            ))
            .sorted(Comparator.comparingDouble(SourceDocument::score).reversed())
            .toList();

        log.info("Retrieved {} chunks", chunks.size());
        return new RetrievalResult(context, sources, query);
    }

    private Document saveDocument(Document document) {
        return documentRepository.save(document);
    }

    private double calculateSimilarity(float[] a, float[] b) {
        if (a == null || b == null) return 0.0;
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
    }
}
