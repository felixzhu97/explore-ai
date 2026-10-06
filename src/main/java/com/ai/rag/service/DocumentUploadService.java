package com.ai.rag.service;

import com.ai.rag.domain.exception.DocumentNotFoundException;
import com.ai.rag.domain.exception.DocumentProcessingException;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.DocumentStatus;
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
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document lifecycle service - handles upload, list, and delete. Uses ETL ports to keep
 * infrastructure details out of the application layer.
 */
@Service
public class DocumentUploadService {

  private static final Logger log = LoggerFactory.getLogger(DocumentUploadService.class);

  public record UploadResult(
      DocumentId documentId,
      String title,
      DocumentStatus status,
      int chunkCount,
      Instant createdAt) {}

  private final DocumentReader reader;
  private final DocumentTransformer transformer;
  private final DocumentWriter writer;
  private final DocumentRepository documentRepository;
  private final DocumentChunkRepository chunkRepository;
  private final TransactionTemplate transactions;

  public DocumentUploadService(
      DocumentReader reader,
      DocumentTransformer transformer,
      DocumentWriter writer,
      DocumentRepository documentRepository,
      DocumentChunkRepository chunkRepository,
      PlatformTransactionManager transactionManager) {
    this.reader = reader;
    this.transformer = transformer;
    this.writer = writer;
    this.documentRepository = documentRepository;
    this.chunkRepository = chunkRepository;
    this.transactions = new TransactionTemplate(transactionManager);
  }

  /**
   * Ingests text content. Not transactional: the FAILED status must commit even when ingestion
   * rolls back.
   */
  public UploadResult upload(
      String title, String fileName, Long fileSize, String content, String ownerKey) {
    return processUpload(title, fileName, fileSize, content.getBytes(), ownerKey);
  }

  /**
   * Ingests file bytes. Not transactional: the FAILED status must commit even when ingestion rolls
   * back.
   */
  public UploadResult upload(
      String title, String fileName, Long fileSize, byte[] fileContent, String ownerKey) {
    return processUpload(title, fileName, fileSize, fileContent, ownerKey);
  }

  /** Ingests the uploaded file, using its file name as the title when none is given. */
  public UploadResult upload(MultipartFile file, String title, String ownerKey) {
    String fileName = file.getOriginalFilename();
    String docTitle = title != null && !title.isBlank() ? title : fileName;
    try {
      return upload(docTitle, fileName, file.getSize(), file.getBytes(), ownerKey);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read file content", e);
    }
  }

  /** Lists all documents of the owner. */
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
    if (fileContent.length == 0) {
      throw new IllegalArgumentException("Uploaded file is empty");
    }
    RagDocument document =
        new RagDocument(DocumentId.generate(), title, fileName, fileSize, ownerKey);
    document.markProcessing();
    RagDocument processing = transactions.execute(status -> documentRepository.save(document));

    try {
      return transactions.execute(status -> ingest(processing, fileContent));
    } catch (RuntimeException e) {
      processing.markFailed();
      transactions.executeWithoutResult(status -> documentRepository.save(processing));
      throw e;
    }
  }

  private UploadResult ingest(RagDocument document, byte[] fileContent) {
    String fileName = document.getFileName();
    RawDocument raw = reader.read(fileContent, fileName);
    List<RawDocument> chunkDocs = raw.content().isBlank() ? List.of() : transformer.transform(raw);
    if (chunkDocs.isEmpty()) {
      throw new DocumentProcessingException("No text found in " + fileName);
    }

    List<DocumentChunk> chunks = new ArrayList<>();
    for (int i = 0; i < chunkDocs.size(); i++) {
      RawDocument chunkDoc = chunkDocs.get(i);
      Map<String, Object> metadata = new HashMap<>(chunkDoc.metadata());
      metadata.put("title", document.getTitle());
      metadata.put("fileName", fileName);
      metadata.put("ownerKey", document.getOwnerKeyValue());

      chunks.add(
          DocumentChunk.create(
              ChunkId.generate(), document.getId(), chunkDoc.content(), i, metadata));
    }

    writer.write(chunks);
    document.markReady();
    RagDocument ready = documentRepository.save(document);
    return new UploadResult(
        ready.getId(), ready.getTitle(), ready.getStatus(), chunks.size(), ready.getCreatedAt());
  }
}
