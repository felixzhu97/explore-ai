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
import com.ai.rag.domain.vo.DocumentId;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
  public UploadResult upload(String title, String fileName, String content, String ownerKey) {
    return upload(title, fileName, content.getBytes(StandardCharsets.UTF_8), ownerKey);
  }

  /**
   * Ingests file bytes. Not transactional: the FAILED status must commit even when ingestion rolls
   * back.
   */
  public UploadResult upload(String title, String fileName, byte[] fileContent, String ownerKey) {
    RagDocument document =
        RagDocument.startIngestion(title, fileName, fileContent.length, ownerKey);
    RagDocument processing = transactions.execute(status -> documentRepository.save(document));

    try {
      return transactions.execute(status -> ingest(processing, fileContent));
    } catch (RuntimeException e) {
      recordFailure(processing, e);
      throw e;
    }
  }

  /** Ingests the uploaded file; the title falls back to its file name. */
  public UploadResult upload(MultipartFile file, String title, String ownerKey) {
    try {
      return upload(title, file.getOriginalFilename(), file.getBytes(), ownerKey);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read file content", e);
    }
  }

  /** Lists all documents of the owner. */
  @Transactional(readOnly = true)
  public List<RagDocument> listAll(String ownerKey) {
    return documentRepository.findAllByOwnerKey(ownerKey);
  }

  /** Deletes the owner's document and all its chunks; throws if the document is not found. */
  @Transactional
  public void delete(UUID documentId, String ownerKey) {
    RagDocument document =
        documentRepository
            .findByIdAndOwnerKey(documentId, ownerKey)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));
    chunkRepository.deleteChunksByDocumentId(document.getId());
    documentRepository.deleteByIdAndOwnerKey(documentId, ownerKey);
  }

  private void recordFailure(RagDocument document, RuntimeException cause) {
    try {
      document.failIngestion();
      transactions.executeWithoutResult(status -> documentRepository.save(document));
    } catch (RuntimeException suppressed) {
      cause.addSuppressed(suppressed);
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
      chunks.add(document.newChunk(i, chunkDoc.content(), chunkDoc.metadata()));
    }

    writer.write(chunks);
    document.completeIngestion(chunks.size());
    RagDocument ready = documentRepository.save(document);
    return new UploadResult(
        ready.getId(),
        ready.getTitle(),
        ready.getStatus(),
        ready.getChunkCount(),
        ready.getCreatedAt());
  }
}
