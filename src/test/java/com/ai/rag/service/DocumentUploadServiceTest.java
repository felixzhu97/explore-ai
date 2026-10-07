package com.ai.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.exception.DomainException;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentUploadService")
class DocumentUploadServiceTest {

  @Mock private DocumentReader reader;

  @Mock private DocumentTransformer transformer;

  @Mock private DocumentWriter writer;

  @Mock private DocumentRepository documentRepository;

  @Mock private DocumentChunkRepository chunkRepository;

  @Mock private MultipartFile multipartFile;

  private DocumentUploadService service;

  @BeforeEach
  void setUp() {
    service =
        new DocumentUploadService(
            reader,
            transformer,
            writer,
            documentRepository,
            chunkRepository,
            mock(PlatformTransactionManager.class));
  }

  @Nested
  @DisplayName("upload() - String content")
  class UploadWithStringContent {

    @Test
    @DisplayName("should upload document with string content")
    void shouldUploadDocumentWithStringContent() {
      String title = "Test Document";
      String fileName = "test.txt";
      String content = "This is test content";

      when(documentRepository.save(any(RagDocument.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reader.read(any(byte[].class), eq(fileName)))
          .thenReturn(new RawDocument(content, Map.of("fileName", fileName), fileName));
      when(transformer.transform(any(RawDocument.class)))
          .thenReturn(
              List.of(
                  new RawDocument("chunk1", Map.of("fileName", fileName), fileName),
                  new RawDocument("chunk2", Map.of("fileName", fileName), fileName)));
      doNothing().when(writer).write(any());

      DocumentUploadService.UploadResult result =
          service.upload(title, fileName, content, "c:test-owner");

      assertThat(result.title()).isEqualTo(title);
      assertThat(result.status()).isEqualTo(DocumentStatus.READY);
      assertThat(result.chunkCount()).isEqualTo(2);
      assertThat(result.documentId()).isNotNull();
    }

    @Test
    @DisplayName("should save the document while processing and again when ready")
    void shouldSaveTheDocumentWhileProcessingAndAgainWhenReady() {
      String content = "Test content";

      when(documentRepository.save(any(RagDocument.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reader.read(any(byte[].class), any()))
          .thenReturn(new RawDocument(content, Map.of(), "test"));
      when(transformer.transform(any(RawDocument.class)))
          .thenReturn(List.of(new RawDocument("chunk", Map.of(), "test")));
      doNothing().when(writer).write(any());

      service.upload("Title", "file.txt", content, "c:test-owner");

      verify(documentRepository, times(2)).save(any(RagDocument.class));
    }
  }

  @Nested
  @DisplayName("upload() - byte[] content")
  class UploadWithByteArrayContent {

    @Test
    @DisplayName("should upload document with byte array content")
    void shouldUploadDocumentWithByteArrayContent() {
      String title = "Test Document";
      String fileName = "test.txt";
      byte[] content = "Test content".getBytes();

      when(documentRepository.save(any(RagDocument.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reader.read(eq(content), eq(fileName)))
          .thenReturn(new RawDocument("processed", Map.of("fileName", fileName), fileName));
      when(transformer.transform(any(RawDocument.class)))
          .thenReturn(List.of(new RawDocument("chunk", Map.of("fileName", fileName), fileName)));
      doNothing().when(writer).write(any());

      DocumentUploadService.UploadResult result =
          service.upload(title, fileName, content, "c:test-owner");

      assertThat(result.status()).isEqualTo(DocumentStatus.READY);
      verify(reader).read(eq(content), eq(fileName));
    }

    @Test
    @DisplayName("should mark document as FAILED when the reader cannot extract text")
    void shouldMarkDocumentAsFailedWhenTheReaderCannotExtractText() {
      String fileName = "document.pdf";
      byte[] pdfContent = new byte[] {1, 2, 3};

      when(documentRepository.save(any(RagDocument.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reader.read(eq(pdfContent), eq(fileName)))
          .thenThrow(
              DomainException.unprocessable(
                  "DOCUMENT_UNREADABLE", "Could not extract text from document.pdf"));

      assertThatThrownBy(() -> service.upload("PDF", fileName, pdfContent, "c:test-owner"))
          .isInstanceOf(DomainException.class)
          .hasFieldOrPropertyWithValue("code", "DOCUMENT_UNREADABLE")
          .hasMessage("Could not extract text from document.pdf");

      verify(documentRepository, times(2)).save(any(RagDocument.class));
    }

    @Test
    @DisplayName("should reject blank text without calling the transformer")
    void shouldRejectBlankTextWithoutCallingTheTransformer() {
      when(documentRepository.save(any(RagDocument.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reader.read(any(byte[].class), eq("blank.txt")))
          .thenReturn(new RawDocument(" \n", Map.of(), "blank.txt"));

      assertThatThrownBy(() -> service.upload("Blank", "blank.txt", " \n", "c:test-owner"))
          .isInstanceOf(DomainException.class)
          .hasFieldOrPropertyWithValue("code", "DOCUMENT_UNREADABLE")
          .hasMessage("No text found in blank.txt");

      verifyNoInteractions(transformer, writer);
    }

    @Test
    @DisplayName("should reject an empty file before storing anything")
    void shouldRejectAnEmptyFileBeforeStoringAnything() {
      assertThatThrownBy(() -> service.upload("Empty", "empty.txt", new byte[0], "c:test-owner"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Uploaded file is empty");

      verifyNoInteractions(documentRepository, reader);
    }
  }

  @Nested
  @DisplayName("upload() - MultipartFile")
  class UploadWithMultipartFile {

    @Test
    @DisplayName("should upload document from MultipartFile with custom title")
    void shouldUploadDocumentFromMultipartFileWithCustomTitle() throws IOException {
      String customTitle = "Custom Title";
      String originalFileName = "original.txt";

      when(multipartFile.getOriginalFilename()).thenReturn(originalFileName);
      when(multipartFile.getBytes()).thenReturn("content".getBytes());
      when(documentRepository.save(any(RagDocument.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reader.read(any(byte[].class), eq(originalFileName)))
          .thenReturn(
              new RawDocument("content", Map.of("fileName", originalFileName), originalFileName));
      when(transformer.transform(any(RawDocument.class)))
          .thenReturn(
              List.of(
                  new RawDocument(
                      "chunk", Map.of("fileName", originalFileName), originalFileName)));
      doNothing().when(writer).write(any());

      DocumentUploadService.UploadResult result =
          service.upload(multipartFile, customTitle, "c:test-owner");

      assertThat(result.title()).isEqualTo(customTitle);
    }

    @Test
    @DisplayName("should use file name as title when title is null")
    void shouldUseFileNameAsTitleWhenTitleIsNull() throws IOException {
      String originalFileName = "my-document.txt";

      when(multipartFile.getOriginalFilename()).thenReturn(originalFileName);
      when(multipartFile.getBytes()).thenReturn("content".getBytes());
      when(documentRepository.save(any(RagDocument.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reader.read(any(byte[].class), eq(originalFileName)))
          .thenReturn(
              new RawDocument("content", Map.of("fileName", originalFileName), originalFileName));
      when(transformer.transform(any(RawDocument.class)))
          .thenReturn(
              List.of(
                  new RawDocument(
                      "chunk", Map.of("fileName", originalFileName), originalFileName)));
      doNothing().when(writer).write(any());

      DocumentUploadService.UploadResult result =
          service.upload(multipartFile, null, "c:test-owner");

      assertThat(result.title()).isEqualTo(originalFileName);
    }

    @Test
    @DisplayName("should throw exception when file read fails")
    void shouldThrowExceptionWhenFileReadFails() throws IOException {
      when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
      when(multipartFile.getBytes()).thenThrow(new IOException("File read error"));

      assertThatThrownBy(() -> service.upload(multipartFile, "Title", "c:test-owner"))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Failed to read file content");
    }
  }

  @Nested
  @DisplayName("upload() - Error handling")
  class UploadErrorHandling {

    @Test
    @DisplayName("should mark document as FAILED when transformer fails")
    void shouldMarkDocumentAsFailedWhenTransformerFails() {
      String content = "Test content";
      when(documentRepository.save(any(RagDocument.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reader.read(any(byte[].class), any()))
          .thenReturn(new RawDocument(content, Map.of(), "test"));
      when(transformer.transform(any(RawDocument.class)))
          .thenThrow(new RuntimeException("Transformation failed"));

      assertThatThrownBy(() -> service.upload("Title", "file.txt", content, "c:test-owner"))
          .hasMessage("Transformation failed");

      verify(documentRepository, times(2)).save(any(RagDocument.class));
    }

    @Test
    @DisplayName("should mark document as FAILED when writer fails")
    void shouldMarkDocumentAsFailedWhenWriterFails() {
      String content = "Test content";
      when(documentRepository.save(any(RagDocument.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reader.read(any(byte[].class), any()))
          .thenReturn(new RawDocument(content, Map.of(), "test"));
      when(transformer.transform(any(RawDocument.class)))
          .thenReturn(List.of(new RawDocument("chunk", Map.of(), "test")));
      doThrow(new RuntimeException("Embedding failed")).when(writer).write(any());

      assertThatThrownBy(() -> service.upload("Title", "file.txt", content, "c:test-owner"))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("Embedding failed");

      ArgumentCaptor<RagDocument> saved = ArgumentCaptor.forClass(RagDocument.class);
      verify(documentRepository, times(2)).save(saved.capture());
      assertThat(saved.getValue().getStatus()).isEqualTo(DocumentStatus.FAILED);
    }

    @Test
    @DisplayName("should keep the ingestion error when saving the failure also fails")
    void shouldKeepTheIngestionErrorWhenSavingTheFailureAlsoFails() {
      String content = "Test content";
      RuntimeException saveFailure = new IllegalStateException("database down");
      when(documentRepository.save(any(RagDocument.class)))
          .thenAnswer(invocation -> invocation.getArgument(0))
          .thenThrow(saveFailure);
      when(reader.read(any(byte[].class), any()))
          .thenReturn(new RawDocument(content, Map.of(), "test"));
      when(transformer.transform(any(RawDocument.class)))
          .thenThrow(new RuntimeException("Transformation failed"));

      assertThatThrownBy(() -> service.upload("Title", "file.txt", content, "c:test-owner"))
          .hasMessage("Transformation failed")
          .hasSuppressedException(saveFailure);
    }

    @Test
    @DisplayName("should hand the writer chunks owned by the uploader")
    void shouldHandTheWriterChunksOwnedByTheUploader() {
      when(documentRepository.save(any(RagDocument.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));
      when(reader.read(any(byte[].class), any()))
          .thenReturn(new RawDocument("text", Map.of(), "test"));
      when(transformer.transform(any(RawDocument.class)))
          .thenReturn(List.of(new RawDocument("chunk", Map.of("page", 1), "test")));

      service.upload("Title", "file.txt", "text", "c:test-owner");

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<DocumentChunk>> written = ArgumentCaptor.forClass(List.class);
      verify(writer).write(written.capture());
      DocumentChunk chunk = written.getValue().getFirst();
      assertThat(chunk.getOwnerKey()).isEqualTo(OwnerKey.parse("c:test-owner"));
      assertThat(chunk.getMetadata())
          .containsEntry("page", 1)
          .containsEntry("title", "Title")
          .containsEntry("fileName", "file.txt");
    }
  }

  @Nested
  @DisplayName("listAll()")
  class ListAll {

    @Test
    @DisplayName("should return all documents")
    void shouldReturnAllDocuments() {
      RagDocument doc1 = RagDocument.startIngestion("Doc1", "file1.txt", 100L, "c:test");
      RagDocument doc2 = RagDocument.startIngestion("Doc2", "file2.txt", 200L, "c:test");
      when(documentRepository.findAllByOwnerKey("c:test-owner")).thenReturn(List.of(doc1, doc2));

      List<RagDocument> result = service.listAll("c:test-owner");

      assertThat(result).hasSize(2);
      verify(documentRepository).findAllByOwnerKey("c:test-owner");
    }

    @Test
    @DisplayName("should return empty list when no documents")
    void shouldReturnEmptyListWhenNoDocuments() {
      when(documentRepository.findAllByOwnerKey("c:test-owner")).thenReturn(List.of());

      List<RagDocument> result = service.listAll("c:test-owner");

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("delete()")
  class Delete {

    @Test
    @DisplayName("should delete document and its chunks")
    void shouldDeleteDocumentAndItsChunks() {
      UUID documentId = UUID.randomUUID();
      DocumentId docId = DocumentId.of(documentId);
      RagDocument document = stored(docId, DocumentStatus.READY, 2);

      when(documentRepository.findByIdAndOwnerKey(documentId, "c:test-owner"))
          .thenReturn(Optional.of(document));

      service.delete(documentId, "c:test-owner");

      verify(chunkRepository).deleteChunksByDocumentId(docId);
      verify(documentRepository).deleteByIdAndOwnerKey(documentId, "c:test-owner");
    }

    @Test
    @DisplayName("should throw exception when document not found")
    void shouldThrowExceptionWhenDocumentNotFound() {
      UUID documentId = UUID.randomUUID();
      when(documentRepository.findByIdAndOwnerKey(documentId, "c:test-owner"))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.delete(documentId, "c:test-owner"))
          .isInstanceOf(DomainException.class)
          .hasFieldOrPropertyWithValue("code", "DOCUMENT_NOT_FOUND");
    }

    @Test
    @DisplayName("should delete chunks even when document has no chunks")
    void shouldDeleteChunksEvenWhenDocumentHasNoChunks() {
      UUID documentId = UUID.randomUUID();
      DocumentId docId = DocumentId.of(documentId);
      RagDocument document = stored(docId, DocumentStatus.FAILED, 0);

      when(documentRepository.findByIdAndOwnerKey(documentId, "c:test-owner"))
          .thenReturn(Optional.of(document));

      service.delete(documentId, "c:test-owner");

      verify(chunkRepository).deleteChunksByDocumentId(docId);
      verify(documentRepository).deleteByIdAndOwnerKey(documentId, "c:test-owner");
    }

    private RagDocument stored(DocumentId docId, DocumentStatus status, int chunkCount) {
      Instant now = Instant.now();
      return RagDocument.restore(
          docId, "Test Doc", "test.txt", 100L, status, chunkCount, now, now, "c:test");
    }
  }
}
