package com.ai.rag.application.usecase;

import com.ai.rag.infrastructure.parser.PdfTextExtractor;
import com.ai.rag.domain.vo.DocumentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentUploadUseCase {

    private static final Logger log = LoggerFactory.getLogger(DocumentUploadUseCase.class);

    private final RagApplicationService ragApplicationService;
    private final PdfTextExtractor pdfTextExtractor;

    public DocumentUploadUseCase(RagApplicationService ragApplicationService, PdfTextExtractor pdfTextExtractor) {
        this.ragApplicationService = ragApplicationService;
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

        try {
            byte[] fileBytes = file.getBytes();
            String content = extractContent(fileBytes, fileName);
            var result = ragApplicationService.uploadDocument(docTitle, fileName, file.getSize(), content);

            return new UploadResult(
                    result.documentId(),
                    result.title(),
                    result.status(),
                    result.chunkCount()
            );
        } catch (IOException e) {
            log.error("Failed to read file content: {}", e.getMessage());
            throw new DocumentUploadException("Failed to read file content: " + e.getMessage(), e);
        }
    }

    private String extractContent(byte[] fileBytes, String fileName) {
        String extension = pdfTextExtractor.getExtension(fileName);
        if ("pdf".equalsIgnoreCase(extension)) {
            return pdfTextExtractor.extractText(fileBytes)
                    .orElseThrow(() -> {
                        log.error("Failed to extract text from PDF: {}", fileName);
                        return new DocumentUploadException(
                                "Failed to extract text from PDF file: " + fileName,
                                new IllegalStateException("PDF text extraction returned empty result")
                        );
                    });
        }
        return new String(fileBytes, StandardCharsets.UTF_8);
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
