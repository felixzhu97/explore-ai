package com.ai.rag.service;

import com.ai.rag.domain.model.RagDocument;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * RAG application service. Delegates to DocumentUploadService (upload/list/delete) and
 * DocumentSearchService (retrieve).
 */
@Service
@RequiredArgsConstructor
public class RagApplicationService {
  public record RetrievalResult(
      String context, List<SourceDocument> sources, String enrichedQuery) {}

  private final DocumentUploadService uploadService;
  private final DocumentSearchService searchService;

  public DocumentUploadService.UploadResult uploadDocument(
      String title, String fileName, Long fileSize, String content, String ownerKey) {
    return uploadService.upload(title, fileName, fileSize, content, ownerKey);
  }

  public DocumentUploadService.UploadResult uploadDocument(
      org.springframework.web.multipart.MultipartFile file, String title, String ownerKey) {
    return uploadService.upload(file, title, ownerKey);
  }

  public DocumentUploadService.UploadResult uploadDocumentFromBytes(
      String title, String fileName, Long fileSize, byte[] fileContent, String ownerKey) {
    return uploadService.upload(title, fileName, fileSize, fileContent, ownerKey);
  }

  public List<RagDocument> listDocuments(String ownerKey) {
    return uploadService.listAll(ownerKey);
  }

  public Map<DocumentId, Integer> chunkCounts(List<RagDocument> documents) {
    return uploadService.chunkCounts(documents);
  }

  public void deleteDocument(UUID documentId, String ownerKey) {
    uploadService.delete(documentId, ownerKey);
  }

  public RetrievalResult retrieveContext(
      String query, List<DocumentId> documentIds, int topK, String ownerKey) {
    var result = searchService.retrieve(query, documentIds, topK, ownerKey);
    return new RetrievalResult(result.context(), result.sources(), query);
  }
}
