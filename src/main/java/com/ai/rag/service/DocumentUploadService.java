package com.ai.rag.service;

import com.ai.rag.domain.exception.DocumentNotFoundException;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.RagDocument;
import com.ai.rag.domain.model.RawDocument;
import com.ai.rag.domain.repository.DocumentChunkRepository;
import com.ai.rag.domain.repository.DocumentReader;
import com.ai.rag.domain.repository.DocumentRepository;
import com.ai.rag.domain.repository.DocumentTransformer;
import com.ai.rag.domain.repository.DocumentWriter;
import com.ai.rag.domain.vo.ChunkId;
import com.ai.rag.domain.vo.DocumentId;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document lifecycle service - handles upload, list, and delete. Uses ETL ports to keep
 * infrastructure details out of the application layer.
 */
@Service
public class DocumentUploadService {

  private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);

  public record UploadResult(DocumentId documentId, String title, String status, int chunkCount) {}

  private final DocumentReader reader;
  private final DocumentTransformer transformer;
  private final DocumentWriter writer;
  private final DocumentRepository documentRepository;
  private final DocumentChunkRepository chunkRepository;

  public DocumentUploadService(
      DocumentReader reader,
      DocumentTransformer transformer,
      DocumentWriter writer,
      DocumentRepository documentRepository,
      DocumentChunkRepository chunkRepository) {
    this.reader = reader;
    this.transformer = transformer;
    this.writer = writer;
    this.documentRepository = documentRepository;
    this.chunkRepository = chunkRepository;
  }

  @Transactional
  public UploadResult upload(
      String title, String fileName, Long fileSize, String content, String ownerKey) {
    return processUpload(title, fileName, fileSize, content.getBytes(), ownerKey);
  }

  @Transactional
  public UploadResult upload(
      String title, String fileName, Long fileSize, byte[] fileContent, String ownerKey) {
    return processUpload(title, fileName, fileSize, fileContent, ownerKey);
  }

  /** Ingests the uploaded file, using its file name as the title when none is given. */
  @Transactional
  public UploadResult upload(MultipartFile file, String title, String ownerKey) {
    String fileName = file.getOriginalFilename();
    String docTitle = title != null && !title.isBlank() ? title : fileName;
    try {
      return upload(docTitle, fileName, file.getSize(), file.getBytes(), ownerKey);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read file content", e);
    }
  }

  @Transactional(readOnly = true)
  public List<RagDocument> listAll(String ownerKey) {
    return documentRepository.findAllByOwnerKey(ownerKey);
  }

  /** Counts stored chunks for each document; documents without chunks map to 0. */
  @Transactional(readOnly = true)
  public Map<DocumentId, Integer> chunkCounts(List<RagDocument> documents) {
    List<DocumentId> ids = documents.stream().map(RagDocument::getId).toList();
    Map<DocumentId, Integer> stored = chunkRepository.countChunksByDocumentIds(ids);
    Map<DocumentId, Integer> counts = new HashMap<>();
    ids.forEach(id -> counts.put(id, stored.getOrDefault(id, 0)));
    return counts;
  }

  /** Deletes the owner's document and all its chunks; throws if the document is not found. */
  @Transactional
  public void delete(UUID documentId, String ownerKey) {
    DocumentId docId = DocumentId.of(documentId);
    documentRepository
        .findByIdAndOwnerKey(documentId, ownerKey)
        .orElseThrow(() -> new DocumentNotFoundException(documentId));
    log.info(
        "Deleting document {} with {} chunks",
        documentId,
        chunkRepository.findChunksByDocumentId(docId).size());
    chunkRepository.deleteChunksByDocumentId(docId);
    documentRepository.deleteByIdAndOwnerKey(documentId, ownerKey);
  }

  private UploadResult processUpload(
      String title, String fileName, Long fileSize, byte[] fileContent, String ownerKey) {
    RagDocument document =
        new RagDocument(DocumentId.generate(), title, fileName, fileSize, ownerKey);
    document.markProcessing();
    document = documentRepository.save(document);

    try {
      RawDocument raw = reader.read(fileContent, fileName);
      List<RawDocument> chunkDocs = transformer.transform(raw);

      List<DocumentChunk> chunks = new ArrayList<>();
      for (int i = 0; i < chunkDocs.size(); i++) {
        RawDocument chunkDoc = chunkDocs.get(i);
        Map<String, Object> metadata = new HashMap<>(chunkDoc.metadata());
        metadata.put("title", document.getTitle());
        metadata.put("fileName", document.getFileName());
        metadata.put("ownerKey", ownerKey);

        chunks.add(
            DocumentChunk.create(
                ChunkId.generate(), document.getId(), chunkDoc.content(), i, metadata));
      }

      writer.write(chunks);
      document.markReady();
      document = documentRepository.save(document);
      return new UploadResult(
          document.getId(), document.getTitle(), document.getStatus().name(), chunks.size());
    } catch (Exception e) {
      log.error("Failed to process document", e);
      document.markFailed();
      documentRepository.save(document);
      throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
    }
  }
}
