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

  /** Uploads a text document. */
  public DocumentUploadService.UploadResult uploadDocument(
      String title, String fileName, Long fileSize, String content, String ownerKey) {
    return uploadService.upload(title, fileName, fileSize, content, ownerKey);
  }

  /** Uploads a multipart file. */
  public DocumentUploadService.UploadResult uploadDocument(
      org.springframework.web.multipart.MultipartFile file, String title, String ownerKey) {
    return uploadService.upload(file, title, ownerKey);
  }

  /** Uploads a document from bytes. */
  public DocumentUploadService.UploadResult uploadDocumentFromBytes(
      String title, String fileName, Long fileSize, byte[] fileContent, String ownerKey) {
    return uploadService.upload(title, fileName, fileSize, fileContent, ownerKey);
  }

  /** Lists the owner's documents. */
  public List<RagDocument> listDocuments(String ownerKey) {
    return uploadService.listAll(ownerKey);
  }

  /** Counts the chunks of each document. */
  public Map<DocumentId, Integer> chunkCounts(List<RagDocument> documents) {
    return uploadService.chunkCounts(documents);
  }

  /** Deletes the owner's document. */
  public void deleteDocument(UUID documentId, String ownerKey) {
    uploadService.delete(documentId, ownerKey);
  }

  /** Retrieves the chunks that match the query. */
  public RetrievalResult retrieveContext(
      String query, List<DocumentId> documentIds, int topK, String ownerKey) {
    var result = searchService.retrieve(query, documentIds, topK, ownerKey);
    return new RetrievalResult(result.context(), result.sources(), query);
  }
}
