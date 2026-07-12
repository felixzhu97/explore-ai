package com.ai.rag.application.usecase;

import com.ai.rag.domain.exception.DocumentNotFoundException;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.model.RawDocument;
import com.ai.rag.domain.repository.DocumentReader;
import com.ai.rag.domain.repository.DocumentTransformer;
import com.ai.rag.domain.repository.DocumentWriter;
import com.ai.rag.domain.repository.IDocumentChunkRepository;
import com.ai.rag.domain.repository.IDocumentRepository;
import com.ai.rag.domain.vo.DocumentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentUploadService")
class DocumentUploadServiceTest {

    @Mock
    private DocumentReader reader;

    @Mock
    private DocumentTransformer transformer;

    @Mock
    private DocumentWriter writer;

    @Mock
    private IDocumentRepository documentRepository;

    @Mock
    private IDocumentChunkRepository chunkRepository;

    @Mock
    private MultipartFile multipartFile;

    private DocumentUploadService service;

    @BeforeEach
    void setUp() {
        service = new DocumentUploadService(reader, transformer, writer, documentRepository, chunkRepository);
    }

    @Nested
    @DisplayName("upload() - String content")
    class UploadWithStringContent {

        @Test
        @DisplayName("should upload document with string content")
        void shouldUploadDocumentWithStringContent() {
            String title = "Test Document";
            String fileName = "test.txt";
            Long fileSize = 1024L;
            String content = "This is test content";

            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(reader.read(any(byte[].class), eq(fileName)))
                    .thenReturn(new RawDocument(content, Map.of("fileName", fileName), fileName));
            when(transformer.transform(any(RawDocument.class)))
                    .thenReturn(List.of(
                            new RawDocument("chunk1", Map.of("fileName", fileName), fileName),
                            new RawDocument("chunk2", Map.of("fileName", fileName), fileName)
                    ));
            doNothing().when(writer).write(any());

            DocumentUploadService.UploadResult result = service.upload(title, fileName, fileSize, content);

            assertThat(result.title()).isEqualTo(title);
            assertThat(result.status()).isEqualTo("READY");
            assertThat(result.chunkCount()).isEqualTo(2);
            assertThat(result.documentId()).isNotNull();
        }

        @Test
        @DisplayName("should mark document as UPLOADING then READY")
        void shouldMarkDocumentAsUploadingThenReady() {
            String content = "Test content";

            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(reader.read(any(byte[].class), any()))
                    .thenReturn(new RawDocument(content, Map.of(), "test"));
            when(transformer.transform(any(RawDocument.class)))
                    .thenReturn(List.of(new RawDocument("chunk", Map.of(), "test")));
            doNothing().when(writer).write(any());

            service.upload("Title", "file.txt", 100L, content);

            verify(documentRepository, times(2)).save(any(Document.class));
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

            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(reader.read(eq(content), eq(fileName)))
                    .thenReturn(new RawDocument("processed", Map.of("fileName", fileName), fileName));
            when(transformer.transform(any(RawDocument.class)))
                    .thenReturn(List.of(new RawDocument("chunk", Map.of("fileName", fileName), fileName)));
            doNothing().when(writer).write(any());

            DocumentUploadService.UploadResult result = service.upload(title, fileName, 12L, content);

            assertThat(result.status()).isEqualTo("READY");
            verify(reader).read(eq(content), eq(fileName));
        }

        @Test
        @DisplayName("should throw exception when reader returns empty content")
        void shouldThrowExceptionWhenReaderReturnsEmptyContent() {
            String fileName = "document.pdf";
            byte[] pdfContent = new byte[]{1, 2, 3};

            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(reader.read(eq(pdfContent), eq(fileName)))
                    .thenThrow(new IllegalStateException("PDF text extraction returned empty"));

            assertThatThrownBy(() -> service.upload("PDF", fileName, 3L, pdfContent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process document");
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
            when(multipartFile.getSize()).thenReturn(100L);
            when(multipartFile.getBytes()).thenReturn("content".getBytes());
            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(reader.read(any(byte[].class), eq(originalFileName)))
                    .thenReturn(new RawDocument("content", Map.of("fileName", originalFileName), originalFileName));
            when(transformer.transform(any(RawDocument.class)))
                    .thenReturn(List.of(new RawDocument("chunk", Map.of("fileName", originalFileName), originalFileName)));
            doNothing().when(writer).write(any());

            DocumentUploadService.UploadResult result = service.upload(multipartFile, customTitle);

            assertThat(result.title()).isEqualTo(customTitle);
        }

        @Test
        @DisplayName("should use file name as title when title is null")
        void shouldUseFileNameAsTitleWhenTitleIsNull() throws IOException {
            String originalFileName = "my-document.txt";

            when(multipartFile.getOriginalFilename()).thenReturn(originalFileName);
            when(multipartFile.getSize()).thenReturn(50L);
            when(multipartFile.getBytes()).thenReturn("content".getBytes());
            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(reader.read(any(byte[].class), eq(originalFileName)))
                    .thenReturn(new RawDocument("content", Map.of("fileName", originalFileName), originalFileName));
            when(transformer.transform(any(RawDocument.class)))
                    .thenReturn(List.of(new RawDocument("chunk", Map.of("fileName", originalFileName), originalFileName)));
            doNothing().when(writer).write(any());

            DocumentUploadService.UploadResult result = service.upload(multipartFile, null);

            assertThat(result.title()).isEqualTo(originalFileName);
        }

        @Test
        @DisplayName("should throw exception when file read fails")
        void shouldThrowExceptionWhenFileReadFails() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
            when(multipartFile.getSize()).thenReturn(100L);
            when(multipartFile.getBytes()).thenThrow(new IOException("File read error"));

            assertThatThrownBy(() -> service.upload(multipartFile, "Title"))
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
            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(reader.read(any(byte[].class), any()))
                    .thenReturn(new RawDocument(content, Map.of(), "test"));
            when(transformer.transform(any(RawDocument.class)))
                    .thenThrow(new RuntimeException("Transformation failed"));

            assertThatThrownBy(() -> service.upload("Title", "file.txt", 100L, content))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process document");

            verify(documentRepository, times(2)).save(any(Document.class));
        }

        @Test
        @DisplayName("should mark document as FAILED when writer fails")
        void shouldMarkDocumentAsFailedWhenWriterFails() {
            String content = "Test content";
            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(reader.read(any(byte[].class), any()))
                    .thenReturn(new RawDocument(content, Map.of(), "test"));
            when(transformer.transform(any(RawDocument.class)))
                    .thenReturn(List.of(new RawDocument("chunk", Map.of(), "test")));
            doThrow(new RuntimeException("Embedding failed")).when(writer).write(any());

            assertThatThrownBy(() -> service.upload("Title", "file.txt", 100L, content))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Embedding failed");

            verify(documentRepository, times(2)).save(any(Document.class));
        }
    }

    @Nested
    @DisplayName("listAll()")
    class ListAll {

        @Test
        @DisplayName("should return all documents")
        void shouldReturnAllDocuments() {
            Document doc1 = new Document(DocumentId.generate(), "Doc1", "file1.txt", 100L);
            Document doc2 = new Document(DocumentId.generate(), "Doc2", "file2.txt", 200L);
            when(documentRepository.findAll()).thenReturn(List.of(doc1, doc2));

            List<Document> result = service.listAll();

            assertThat(result).hasSize(2);
            verify(documentRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void shouldReturnEmptyListWhenNoDocuments() {
            when(documentRepository.findAll()).thenReturn(List.of());

            List<Document> result = service.listAll();

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
            Document document = new Document(docId, "Test Doc", "test.txt", 100L);

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
            when(chunkRepository.findChunksByDocumentId(docId)).thenReturn(List.of(
                    createChunk(docId, 0), createChunk(docId, 1)));

            service.delete(documentId);

            verify(chunkRepository).deleteChunksByDocumentId(docId);
            verify(documentRepository).delete(documentId);
        }

        @Test
        @DisplayName("should throw exception when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            UUID documentId = UUID.randomUUID();
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(documentId))
                    .isInstanceOf(DocumentNotFoundException.class);
        }

        @Test
        @DisplayName("should delete chunks even when document has no chunks")
        void shouldDeleteChunksEvenWhenDocumentHasNoChunks() {
            UUID documentId = UUID.randomUUID();
            DocumentId docId = DocumentId.of(documentId);
            Document document = new Document(docId, "Test Doc", "test.txt", 100L);

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
            when(chunkRepository.findChunksByDocumentId(docId)).thenReturn(List.of());

            service.delete(documentId);

            verify(chunkRepository).deleteChunksByDocumentId(docId);
            verify(documentRepository).delete(documentId);
        }

        private DocumentChunk createChunk(DocumentId docId, int index) {
            return DocumentChunk.reconstitute(
                    DocumentId.generate(), docId, "chunk " + index, index,
                    Map.of(), new float[]{0.1f}, Instant.now());
        }
    }
}
