package com.ai.rag.application.usecase;

import com.ai.rag.domain.exception.DocumentNotFoundException;
import com.ai.rag.domain.exception.DocumentProcessingException;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.repository.IDocumentChunkRepository;
import com.ai.rag.domain.repository.IDocumentRepository;
import com.ai.rag.domain.util.VectorSimilarity;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.infrastructure.llm.EmbeddingAdapter;
import com.ai.rag.infrastructure.parser.PdfTextExtractor;
import com.ai.rag.infrastructure.vector.PgVectorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class RagApplicationService {

    private static final Logger log = LoggerFactory.getLogger(RagApplicationService.class);
    private static final int DEFAULT_TOP_K = 5;

    public record RetrievalResult(
            String context,
            List<SourceDocument> sources,
            String enrichedQuery
    ) {}

    public record UploadResult(
            DocumentId documentId,
            String title,
            String status,
            int chunkCount
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

    @Transactional
    public UploadResult uploadDocument(String title, String fileName, Long fileSize, String content) {
        return processUpload(title, fileName, fileSize, () -> content);
    }

    @Transactional
    public UploadResult uploadDocumentFromBytes(String title, String fileName, Long fileSize, byte[] fileContent) {
        return processUpload(title, fileName, fileSize, () -> extractContent(fileContent, fileName));
    }

    @Transactional
    public UploadResult uploadDocument(MultipartFile file, String title) {
        String fileName = file.getOriginalFilename();
        String docTitle = title != null && !title.isBlank() ? title : fileName;

        try {
            byte[] fileBytes = file.getBytes();
            return uploadDocumentFromBytes(docTitle, fileName, file.getSize(), fileBytes);
        } catch (IOException e) {
            throw new DocumentProcessingException("Failed to read file content: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<Document> listDocuments() {
        return documentRepository.findAll();
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        log.info("Deleting document: {}", documentId);

        DocumentId docId = DocumentId.of(documentId);
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        int chunkCount = chunkRepository.findChunksByDocumentId(docId).size();
        log.info("Deleting document {} with {} chunks", documentId, chunkCount);

        chunkRepository.deleteChunksByDocumentId(docId);
        documentRepository.delete(documentId);

        log.info("Document deleted successfully: {}", documentId);
    }

    public RetrievalResult retrieveContext(String query, List<DocumentId> docIds, int topK) {
        log.info("RAG retrieval for query: {}", query);

        float[] queryEmbedding = embeddingAdapter.embed(query);

        int effectiveTopK = topK > 0 ? topK : DEFAULT_TOP_K;

        List<DocumentChunk> chunks;
        if (docIds != null && !docIds.isEmpty()) {
            List<UUID> docUuids = docIds.stream().map(DocumentId::value).toList();
            chunks = vectorAdapter.search(queryEmbedding, effectiveTopK, docUuids);
        } else {
            chunks = vectorAdapter.search(queryEmbedding, effectiveTopK);
        }

        String context = chunks.stream()
            .map(DocumentChunk::getContent)
            .reduce((a, b) -> a + "\n\n" + b)
            .orElse("");

        List<SourceDocument> sources = chunks.stream()
            .map(chunk -> new SourceDocument(
                truncateContent(chunk.getContent()),
                VectorSimilarity.cosineSimilarity(queryEmbedding, chunk.getEmbedding()),
                chunk.getMetadata()
            ))
            .sorted(Comparator.comparingDouble(SourceDocument::score).reversed())
            .toList();

        log.info("Retrieved {} chunks", chunks.size());
        return new RetrievalResult(context, sources, query);
    }

    private UploadResult processUpload(String title, String fileName, Long fileSize, java.util.function.Supplier<String> contentSupplier) {
        Document document = new Document(DocumentId.generate(), title, fileName, fileSize);
        document.markProcessing();
        document = documentRepository.save(document);

        try {
            String content = contentSupplier.get();
            List<String> chunks = chunkingService.chunk(content);
            log.info("Split into {} chunks", chunks.size());

            int chunkCount = saveChunks(title, fileName, document.getId(), chunks);

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

    private String truncateContent(String content) {
        if (content == null || content.length() <= 500) {
            return content;
        }
        return content.substring(0, 500);
    }

    private String extractContent(byte[] fileBytes, String fileName) {
        String extension = pdfTextExtractor.getExtension(fileName);
        if ("pdf".equalsIgnoreCase(extension)) {
            return pdfTextExtractor.extractText(fileBytes)
                    .orElseThrow(() -> new IllegalStateException("PDF text extraction returned empty result"));
        }
        return new String(fileBytes);
    }

    private int saveChunks(String title, String fileName, DocumentId documentId, List<String> chunks) {
        int savedCount = 0;
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            float[] embedding = embeddingAdapter.embed(chunkText);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", title);
            metadata.put("fileName", fileName);

            DocumentChunk chunk = DocumentChunk.reconstitute(
                DocumentId.generate(),
                documentId,
                chunkText,
                i,
                metadata,
                embedding,
                java.time.Instant.now()
            );

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
