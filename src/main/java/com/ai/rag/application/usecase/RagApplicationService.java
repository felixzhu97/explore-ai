package com.ai.rag.application.usecase;

import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAG application service facade.
 * Delegates to DocumentUploadService (upload/list/delete) and DocumentSearchService (retrieve).
 */
@Service
public class RagApplicationService {

    public record RetrievalResult(
            String context,
            List<SourceDocument> sources,
            String enrichedQuery
    ) {}

    private final DocumentUploadService uploadService;
    private final DocumentSearchService searchService;

    public RagApplicationService(DocumentUploadService uploadService, DocumentSearchService searchService) {
        this.uploadService = uploadService;
        this.searchService = searchService;
    }

    public DocumentUploadService.UploadResult uploadDocument(
            String title, String fileName, Long fileSize, String content, String ownerKey) {
        return uploadService.upload(title, fileName, fileSize, content, ownerKey);
    }

    public DocumentUploadService.UploadResult uploadDocumentFromBytes(
            String title, String fileName, Long fileSize, byte[] fileContent, String ownerKey) {
        return uploadService.upload(title, fileName, fileSize, fileContent, ownerKey);
    }

    public DocumentUploadService.UploadResult uploadDocument(
            org.springframework.web.multipart.MultipartFile file, String title, String ownerKey) {
        return uploadService.upload(file, title, ownerKey);
    }

    public List<Document> listDocuments(String ownerKey) {
        return uploadService.listAll(ownerKey);
    }

    public void deleteDocument(UUID documentId, String ownerKey) {
        uploadService.delete(documentId, ownerKey);
    }

    public RetrievalResult retrieveContext(String query, List<DocumentId> docIds, int topK) {
        var result = searchService.retrieve(query, docIds, topK);
        return new RetrievalResult(result.context(), result.sources(), query);
    }
}
