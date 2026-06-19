package com.ai.application.usecase;

import com.ai.adapter.out.document.PdfTextExtractor;
import com.ai.domain.model.Document;
import com.ai.domain.service.RagService;
import com.ai.domain.vo.DocumentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Application layer use case for document upload orchestration.
 * Coordinates between adapters (PdfTextExtractor) and domain services (RagService).
 */
@Service
public class DocumentUploadUseCase {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadUseCase.class);

    private final RagService ragService;
    private final PdfTextExtractor pdfTextExtractor;

    public DocumentUploadUseCase(RagService ragService, PdfTextExtractor pdfTextExtractor) {
        this.ragService = ragService;
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
     * Uploads a document from a multipart file.
     *
     * @param file The uploaded file
     * @param title Optional title (uses filename if null)
     * @return Upload result with document info
     */
    @Transactional
    public UploadResult upload(MultipartFile file, String title) {
        String fileName = file.getOriginalFilename();
        String docTitle = title != null && !title.isBlank() ? title : fileName;

        log.info("Processing document upload: {}", docTitle);

        String content = extractContent(file, fileName);
        Document document = ragService.uploadDocument(docTitle, fileName, file.getSize(), content);

        return new UploadResult(
                document.getId(),
                document.getTitle(),
                document.getStatus().name(),
                0
        );
    }

    private String extractContent(MultipartFile file, String fileName) {
        try {
            byte[] fileBytes = file.getBytes();
            String extension = pdfTextExtractor.getExtension(fileName);

            if ("pdf".equalsIgnoreCase(extension)) {
                return extractPdfContent(fileBytes, fileName);
            }

            return new String(fileBytes, StandardCharsets.UTF_8);

        } catch (IOException e) {
            log.error("Failed to read file content: {}", e.getMessage());
            throw new DocumentUploadException("Failed to read file content: " + e.getMessage(), e);
        }
    }

    private String extractPdfContent(byte[] fileBytes, String fileName) {
        return pdfTextExtractor.extractText(fileBytes)
                .orElseThrow(() -> {
                    log.error("Failed to extract text from PDF: {}", fileName);
                    return new DocumentUploadException(
                            "Failed to extract text from PDF file: " + fileName,
                            new IllegalStateException("PDF text extraction returned empty result")
                    );
                });
    }

    /**
     * Exception thrown when document upload fails.
     */
    public static class DocumentUploadException extends RuntimeException {
        public DocumentUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
