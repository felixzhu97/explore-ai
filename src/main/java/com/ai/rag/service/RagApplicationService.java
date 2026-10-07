package com.ai.rag.service;

import com.ai.rag.domain.model.DocumentId;
import com.ai.rag.domain.model.RagDocument;
import com.ai.rag.domain.model.SourceCitation;
import java.util.List;
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
      String context, List<SourceCitation> sources, String enrichedQuery) {}

  private final DocumentUploadService uploadService;
  private final DocumentSearchService searchService;

  /** Uploads a text document. */
  public DocumentUploadService.UploadResult uploadDocument(
      String title, String fileName, String content, String ownerKey) {
    return uploadService.uploadDocument(title, fileName, content, ownerKey);
  }

  /** Uploads a multipart file. */
  public DocumentUploadService.UploadResult uploadDocument(
      org.springframework.web.multipart.MultipartFile file, String title, String ownerKey) {
    return uploadService.uploadDocument(file, title, ownerKey);
  }

  /** Uploads a document from bytes. */
  public DocumentUploadService.UploadResult uploadDocument(
      String title, String fileName, byte[] fileContent, String ownerKey) {
    return uploadService.uploadDocument(title, fileName, fileContent, ownerKey);
  }

  /** Lists the owner's documents. */
  public List<RagDocument> listDocuments(String ownerKey) {
    return uploadService.listAll(ownerKey);
  }

  /** Lists the owner's documents that are ready to search. */
  public List<RagDocument> listSearchableDocuments(String ownerKey) {
    return uploadService.listAll(ownerKey).stream().filter(RagDocument::isSearchable).toList();
  }

  /** Deletes the owner's document. */
  public void deleteDocument(UUID documentId, String ownerKey) {
    uploadService.deleteDocument(documentId, ownerKey);
  }

  /** Retrieves the chunks that match the query. */
  public RetrievalResult retrieveContext(
      String query, List<DocumentId> documentIds, int topK, String ownerKey) {
    var result = searchService.retrieveDocuments(query, documentIds, topK, ownerKey);
    return new RetrievalResult(result.context(), result.sources(), query);
  }
}
