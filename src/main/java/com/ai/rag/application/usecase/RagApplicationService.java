package com.ai.rag.application.usecase;

import com.ai.rag.domain.exception.DocumentNotFoundException;
import com.ai.rag.domain.exception.DocumentProcessingException;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.repository.IDocumentChunkRepository;
import com.ai.rag.domain.repository.IDocumentRepository;
import com.ai.rag.domain.util.VectorSimilarity;
import com.ai.rag.infrastructure.parser.PdfTextExtractor;
import com.ai.rag.infrastructure.llm.EmbeddingAdapter;
import com.ai.rag.infrastructure.vector.PgVectorAdapter;
import com.ai.rag.domain.vo.DocumentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Application layer service for RAG operations orchestration.
 * Coordinates between domain services, adapters, and handles transactions.
 */
@Service
public class RagApplicationService {

    private static final Logger log = LoggerFactory.getLogger(RagApplicationService.class);

    /**
     * Result of retrieval operation.
     */
    public record RetrievalResult(
        String context,
        List<SourceDocument> sources,
        String enrichedQuery
    ) {}

    private final ChunkingService chunkingService;
    private final EmbeddingAdapter embeddingAdapter;
    private final PgVectorAdapter vectorAdapter;
    private final IDocumentRepository documentRepository;
    private final IDocumentChunkRepository chunkRepository;
    private final PdfTextExtractor pdfTextExtractor;

    public RagApplicationService(
            ChunkingService chunkingService,
            EmbeddingAdapter embeddingAdapter,
            PgVectorAdapter vectorAdapter,
            IDocumentRepository documentRepository,
            IDocumentChunkRepository chunkRepository,
            PdfTextExtractor pdfTextExtractor) {
        this.chunkingService = chunkingService;
        this.embeddingAdapter = embeddingAdapter;
        this.vectorAdapter = vectorAdapter;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    /**
     * Result of document upload operation.
     */
    public record UploadResult(
            DocumentId documentId,
            String title,
            String status,
            int chunkCount
    ) {}

    /**
     * Uploads and processes a document from text content.
     */
    @Transactional
    public UploadResult uploadDocument(String title, String fileName, Long fileSize, String content) {
        log.info("Uploading document: {}", title);

        Document document = new Document(DocumentId.generate(), title, fileName, fileSize);
        document.markProcessing();
        document = documentRepository.save(document);

        try {
            List<String> chunks = chunkingService.chunk(content);
            log.info("Split into {} chunks", chunks.size());

            int chunkCount = saveChunks(title, fileName, document.getId().value(), chunks);

            document.markReady();
            document = documentRepository.save(document);
            log.info("Document uploaded successfully: {}", document.getId());

            return new UploadResult(document.getId(), document.getTitle(), document.getStatus().name(), chunkCount);

        } catch (Exception e) {
            log.error("Failed to process document", e);
            document.markFailed();
            documentRepository.save(document);
            throw new DocumentProcessingException("Failed to process document: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads a document from byte array content.
     */
    @Transactional
    public UploadResult uploadDocumentFromBytes(String title, String fileName, Long fileSize, byte[] fileContent) {
        log.info("Uploading document: {}", title);

        Document document = new Document(DocumentId.generate(), title, fileName, fileSize);
        document.markProcessing();
        document = documentRepository.save(document);

        try {
            String content = extractContent(fileContent, fileName);
            List<String> chunks = chunkingService.chunk(content);
            log.info("Split into {} chunks", chunks.size());

            int chunkCount = saveChunks(title, fileName, document.getId().value(), chunks);

            document.markReady();
            document = documentRepository.save(document);
            log.info("Document uploaded successfully: {}", document.getId());

            return new UploadResult(document.getId(), document.getTitle(), document.getStatus().name(), chunkCount);

        } catch (Exception e) {
            log.error("Failed to process document", e);
            document.markFailed();
            documentRepository.save(document);
            throw new DocumentProcessingException("Failed to process document: " + e.getMessage(), e);
        }
    }

    public List<Document> listDocuments() {
        return documentRepository.findAll();
    }

    /**
     * Deletes a document and its associated chunks.
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        log.info("Deleting document: {}", documentId);

        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        int chunkCount = chunkRepository.findChunksByDocumentId(documentId).size();
        log.info("Deleting document {} with {} chunks", documentId, chunkCount);

        chunkRepository.deleteChunksByDocumentId(documentId);
        documentRepository.delete(documentId);

        log.info("Document deleted successfully: {}", documentId);
    }

    public RetrievalResult retrieveContext(String query, List<UUID> docIds, int topK) {
        log.info("RAG retrieval for query: {}", query);

        float[] queryEmbedding = embeddingAdapter.embed(query);

        List<DocumentChunk> chunks;
        if (docIds != null && !docIds.isEmpty()) {
            chunks = vectorAdapter.search(queryEmbedding, topK, docIds);
        } else {
            chunks = vectorAdapter.search(queryEmbedding, topK);
        }

        String context = chunks.stream()
            .map(DocumentChunk::getContent)
            .reduce((a, b) -> a + "\n\n" + b)
            .orElse("");

        List<SourceDocument> sources = chunks.stream()
            .map(chunk -> {
                String truncated = chunk.getContent().length() > 500
                    ? chunk.getContent().substring(0, 500)
                    : chunk.getContent();
                return new SourceDocument(truncated, VectorSimilarity.cosineSimilarity(queryEmbedding, chunk.getEmbedding()), chunk.getMetadata());
            })
            .sorted(Comparator.comparingDouble(SourceDocument::score).reversed())
            .toList();

        log.info("Retrieved {} chunks", chunks.size());
        return new RetrievalResult(context, sources, query);
    }

    private String extractContent(byte[] fileBytes, String fileName) {
        String extension = pdfTextExtractor.getExtension(fileName);
        if ("pdf".equalsIgnoreCase(extension)) {
            return pdfTextExtractor.extractText(fileBytes)
                    .orElseThrow(() -> new IllegalStateException("PDF text extraction returned empty result"));
        }
        return new String(fileBytes);
    }

    private int saveChunks(String title, String fileName, UUID documentId, List<String> chunks) {
        int savedCount = 0;
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            float[] embedding = embeddingAdapter.embed(chunkText);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", title);
            metadata.put("fileName", fileName);

            DocumentChunk chunk = DocumentChunk.create(
                UUID.randomUUID(),
                documentId,
                chunkText,
                i,
                metadata
            ).withEmbedding(embedding);

            try {
                vectorAdapter.saveChunk(chunk);
                savedCount++;
                log.debug("Chunk {}/{} saved successfully", i + 1, chunks.size());
            } catch (Exception e) {
                log.error("Failed to save chunk {}: {}", i, e.getMessage());
                throw e;
            }
        }
        return savedCount;
    }
}
