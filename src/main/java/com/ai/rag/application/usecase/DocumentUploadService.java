package com.ai.rag.application.usecase;

import com.ai.rag.domain.exception.DocumentNotFoundException;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.repository.IDocumentChunkRepository;
import com.ai.rag.domain.repository.IDocumentRepository;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.infrastructure.llm.EmbeddingAdapter;
import com.ai.rag.infrastructure.parser.PdfTextExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * Document lifecycle service - handles upload, list, and delete.
 */
@Service
public class DocumentUploadService {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);

    public record UploadResult(
            DocumentId documentId,
            String title,
            String status,
            int chunkCount
    ) {}

    private final ChunkingService chunkingService;
    private final EmbeddingAdapter embeddingAdapter;
    private final IDocumentRepository documentRepository;
    private final IDocumentChunkRepository chunkRepository;
    private final PdfTextExtractor pdfTextExtractor;

    public DocumentUploadService(
            ChunkingService chunkingService,
            EmbeddingAdapter embeddingAdapter,
            IDocumentRepository documentRepository,
            IDocumentChunkRepository chunkRepository,
            PdfTextExtractor pdfTextExtractor) {
        this.chunkingService = chunkingService;
        this.embeddingAdapter = embeddingAdapter;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.pdfTextExtractor = pdfTextExtractor;
    }

    @Transactional
    public UploadResult upload(String title, String fileName, Long fileSize, String content) {
        return processUpload(title, fileName, fileSize, () -> content);
    }

    @Transactional
    public UploadResult upload(String title, String fileName, Long fileSize, byte[] fileContent) {
        return processUpload(title, fileName, fileSize, () -> extractContent(fileContent, fileName));
    }

    @Transactional
    public UploadResult upload(MultipartFile file, String title) {
        String fileName = file.getOriginalFilename();
        String docTitle = title != null && !title.isBlank() ? title : fileName;
        try {
            return upload(docTitle, fileName, file.getSize(), file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file content", e);
        }
    }

    @Transactional(readOnly = true)
    public List<Document> listAll() {
        return documentRepository.findAll();
    }

    @Transactional
    public void delete(UUID documentId) {
        DocumentId docId = DocumentId.of(documentId);
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        log.info("Deleting document {} with {} chunks", documentId,
                chunkRepository.findChunksByDocumentId(docId).size());
        chunkRepository.deleteChunksByDocumentId(docId);
        documentRepository.delete(documentId);
    }

    private UploadResult processUpload(String title, String fileName, Long fileSize,
                                       java.util.function.Supplier<String> contentSupplier) {
        Document document = new Document(DocumentId.generate(), title, fileName, fileSize);
        document.markProcessing();
        document = documentRepository.save(document);

        try {
            String content = contentSupplier.get();
            List<String> chunks = chunkingService.chunk(content);
            int chunkCount = saveChunks(title, fileName, document.getId(), chunks);
            document.markReady();
            document = documentRepository.save(document);
            return new UploadResult(document.getId(), document.getTitle(), document.getStatus().name(), chunkCount);
        } catch (Exception e) {
            log.error("Failed to process document", e);
            document.markFailed();
            documentRepository.save(document);
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }

    private int saveChunks(String title, String fileName, DocumentId documentId, List<String> chunks) {
        int count = 0;
        for (int i = 0; i < chunks.size(); i++) {
            float[] embedding = embeddingAdapter.embed(chunks.get(i));
            Map<String, Object> metadata = Map.of("title", title, "fileName", fileName);
            var chunk = com.ai.rag.domain.model.DocumentChunk.reconstitute(
                    DocumentId.generate(), documentId, chunks.get(i), i,
                    metadata, embedding, Instant.now());
            chunkRepository.saveChunk(chunk);
            count++;
        }
        return count;
    }

    private String extractContent(byte[] bytes, String fileName) {
        if ("pdf".equalsIgnoreCase(pdfTextExtractor.getExtension(fileName))) {
            return pdfTextExtractor.extractText(bytes)
                    .orElseThrow(() -> new IllegalStateException("PDF text extraction returned empty"));
        }
        return new String(bytes);
    }
}
