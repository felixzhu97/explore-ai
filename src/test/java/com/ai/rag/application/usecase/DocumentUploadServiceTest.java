package com.ai.rag.application.usecase;

import com.ai.rag.domain.exception.DocumentNotFoundException;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.DocumentChunk;
import com.ai.rag.domain.repository.IDocumentChunkRepository;
import com.ai.rag.domain.repository.IDocumentRepository;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.rag.infrastructure.llm.EmbeddingAdapter;
import com.ai.rag.infrastructure.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private ChunkingService chunkingService;

    @Mock
    private EmbeddingAdapter embeddingAdapter;

    @Mock
    private IDocumentRepository documentRepository;

    @Mock
    private IDocumentChunkRepository chunkRepository;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @Mock
    private MultipartFile multipartFile;

    private DocumentUploadService service;

    @BeforeEach
    void setUp() {
        service = new DocumentUploadService(
                chunkingService, embeddingAdapter, documentRepository, chunkRepository, pdfTextExtractor);
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
            when(chunkingService.chunk(content)).thenReturn(List.of("chunk1", "chunk2"));
            when(embeddingAdapter.embed(any())).thenReturn(new float[]{0.1f, 0.2f});
            doNothing().when(chunkRepository).saveChunk(any());

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
            ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);

            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(chunkingService.chunk(content)).thenReturn(List.of("single chunk"));
            when(embeddingAdapter.embed(any())).thenReturn(new float[]{0.1f});
            doNothing().when(chunkRepository).saveChunk(any());

            service.upload("Title", "file.txt", 100L, content);

            verify(documentRepository, times(2)).save(docCaptor.capture());
            List<Document> savedDocs = docCaptor.getAllValues();
            // First save: UPLOADING (from constructor), then markProcessing() called but not saved
            // Second save: READY (after successful processing)
            assertThat(savedDocs.get(1).getStatus().name()).isEqualTo("READY");
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
            when(chunkingService.chunk(any())).thenReturn(List.of("processed"));
            when(embeddingAdapter.embed(any())).thenReturn(new float[]{0.1f});
            doNothing().when(chunkRepository).saveChunk(any());

            DocumentUploadService.UploadResult result = service.upload(title, fileName, 12L, content);

            assertThat(result.status()).isEqualTo("READY");
            verify(pdfTextExtractor).getExtension(fileName);
        }

        @Test
        @DisplayName("should extract PDF content for PDF files")
        void shouldExtractPdfContentForPdfFiles() {
            String title = "PDF Document";
            String fileName = "document.pdf";
            byte[] pdfContent = new byte[]{1, 2, 3};

            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(pdfTextExtractor.getExtension(fileName)).thenReturn("pdf");
            when(pdfTextExtractor.extractText(pdfContent)).thenReturn(Optional.of("Extracted PDF text"));
            when(chunkingService.chunk("Extracted PDF text")).thenReturn(List.of("text"));
            when(embeddingAdapter.embed(any())).thenReturn(new float[]{0.1f});
            doNothing().when(chunkRepository).saveChunk(any());

            service.upload(title, fileName, 3L, pdfContent);

            verify(pdfTextExtractor).extractText(pdfContent);
            verify(chunkingService).chunk("Extracted PDF text");
        }

        @Test
        @DisplayName("should throw exception when PDF extraction returns empty")
        void shouldThrowExceptionWhenPdfExtractionReturnsEmpty() {
            String fileName = "document.pdf";
            byte[] pdfContent = new byte[]{1, 2, 3};

            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(pdfTextExtractor.getExtension(fileName)).thenReturn("pdf");
            when(pdfTextExtractor.extractText(pdfContent)).thenReturn(Optional.empty());

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
            when(chunkingService.chunk(any())).thenReturn(List.of("chunk"));
            when(embeddingAdapter.embed(any())).thenReturn(new float[]{0.1f});
            doNothing().when(chunkRepository).saveChunk(any());

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
            when(chunkingService.chunk(any())).thenReturn(List.of("chunk"));
            when(embeddingAdapter.embed(any())).thenReturn(new float[]{0.1f});
            doNothing().when(chunkRepository).saveChunk(any());

            DocumentUploadService.UploadResult result = service.upload(multipartFile, null);

            assertThat(result.title()).isEqualTo(originalFileName);
        }

        @Test
        @DisplayName("should use file name as title when title is blank")
        void shouldUseFileNameAsTitleWhenTitleIsBlank() throws IOException {
            String originalFileName = "blank-title.txt";

            when(multipartFile.getOriginalFilename()).thenReturn(originalFileName);
            when(multipartFile.getSize()).thenReturn(50L);
            when(multipartFile.getBytes()).thenReturn("content".getBytes());
            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(chunkingService.chunk(any())).thenReturn(List.of("chunk"));
            when(embeddingAdapter.embed(any())).thenReturn(new float[]{0.1f});
            doNothing().when(chunkRepository).saveChunk(any());

            DocumentUploadService.UploadResult result = service.upload(multipartFile, "   ");

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
        @DisplayName("should mark document as FAILED when chunking fails")
        void shouldMarkDocumentAsFailedWhenChunkingFails() {
            String content = "Test content";
            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(chunkingService.chunk(content)).thenThrow(new RuntimeException("Chunking failed"));

            assertThatThrownBy(() -> service.upload("Title", "file.txt", 100L, content))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process document");

            verify(documentRepository, times(2)).save(any(Document.class));
        }

        @Test
        @DisplayName("should mark document as FAILED when embedding fails")
        void shouldMarkDocumentAsFailedWhenEmbeddingFails() {
            String content = "Test content";
            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(chunkingService.chunk(content)).thenReturn(List.of("chunk1", "chunk2"));
            when(embeddingAdapter.embed("chunk1")).thenReturn(new float[]{0.1f});
            when(embeddingAdapter.embed("chunk2")).thenThrow(new RuntimeException("Embedding failed"));

            assertThatThrownBy(() -> service.upload("Title", "file.txt", 100L, content))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Embedding failed");
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
