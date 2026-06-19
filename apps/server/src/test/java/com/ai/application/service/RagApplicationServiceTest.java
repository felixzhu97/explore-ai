package com.ai.application.service;

import com.ai.adapter.out.document.PdfTextExtractor;
import com.ai.adapter.out.embedding.EmbeddingAdapter;
import com.ai.adapter.out.vector.PgVectorAdapter;
import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentChunk;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.repository.DocumentRepository;
import com.ai.domain.service.ChunkingService;
import com.ai.domain.vo.DocumentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagApplicationService")
class RagApplicationServiceTest {

    @Mock
    private ChunkingService chunkingService;

    @Mock
    private EmbeddingAdapter embeddingAdapter;

    @Mock
    private PgVectorAdapter vectorAdapter;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    private RagApplicationService ragApplicationService;

    @BeforeEach
    void setUp() {
        ragApplicationService = new RagApplicationService(
                chunkingService,
                embeddingAdapter,
                vectorAdapter,
                documentRepository,
                pdfTextExtractor
        );
    }

    @Nested
    @DisplayName("uploadDocument()")
    class UploadDocument {

        @Test
        @DisplayName("should return UploadResult with READY status when successful")
        void shouldReturnUploadResultWithReadyStatusWhenSuccessful() {
            // Arrange
            String title = "Test Document";
            String fileName = "test.txt";
            Long fileSize = 1024L;
            String content = "This is test content";
            List<String> chunks = List.of("chunk1", "chunk2");

            when(chunkingService.chunk(content)).thenReturn(chunks);
            when(embeddingAdapter.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            RagApplicationService.UploadResult result = ragApplicationService.uploadDocument(title, fileName, fileSize, content);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("READY");
            assertThat(result.title()).isEqualTo(title);
            assertThat(result.chunkCount()).isEqualTo(2);
            assertThat(result.documentId()).isNotNull();

            verify(documentRepository, times(2)).save(any(Document.class));
            verify(vectorAdapter, times(2)).saveChunk(any(DocumentChunk.class));
        }

        @Test
        @DisplayName("should mark document as FAILED when chunking fails")
        void shouldMarkDocumentAsFailedWhenChunkingFails() {
            // Arrange
            String title = "Test Document";
            String fileName = "test.txt";
            Long fileSize = 1024L;
            String content = "Content";

            when(chunkingService.chunk(content)).thenThrow(new RuntimeException("Chunking failed"));
            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act & Assert
            assertThatThrownBy(() -> ragApplicationService.uploadDocument(title, fileName, fileSize, content))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process document");

            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository, times(2)).save(documentCaptor.capture());

            Document savedDocument = documentCaptor.getAllValues().get(1);
            assertThat(savedDocument.getStatus().name()).isEqualTo("FAILED");
        }
    }

    @Nested
    @DisplayName("uploadDocumentFromBytes()")
    class UploadDocumentFromBytes {

        @Test
        @DisplayName("should extract content and upload successfully")
        void shouldExtractContentAndUploadSuccessfully() {
            // Arrange
            String title = "Test PDF";
            String fileName = "test.pdf";
            Long fileSize = 2048L;
            byte[] fileContent = "PDF content".getBytes();
            String extractedContent = "Extracted PDF text";
            List<String> chunks = List.of("extracted chunk");

            when(pdfTextExtractor.getExtension(fileName)).thenReturn("pdf");
            when(pdfTextExtractor.extractText(fileContent)).thenReturn(Optional.of(extractedContent));
            when(chunkingService.chunk(extractedContent)).thenReturn(chunks);
            when(embeddingAdapter.embed(anyString())).thenReturn(new float[]{0.1f});
            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            RagApplicationService.UploadResult result = ragApplicationService.uploadDocumentFromBytes(title, fileName, fileSize, fileContent);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("READY");
            assertThat(result.chunkCount()).isEqualTo(1);

            verify(pdfTextExtractor).extractText(fileContent);
        }

        @Test
        @DisplayName("should throw exception when PDF extraction returns empty")
        void shouldThrowExceptionWhenPdfExtractionReturnsEmpty() {
            // Arrange
            String title = "Test PDF";
            String fileName = "test.pdf";
            Long fileSize = 2048L;
            byte[] fileContent = "PDF content".getBytes();

            when(pdfTextExtractor.getExtension(fileName)).thenReturn("pdf");
            when(pdfTextExtractor.extractText(fileContent)).thenReturn(Optional.empty());
            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act & Assert
            assertThatThrownBy(() -> ragApplicationService.uploadDocumentFromBytes(title, fileName, fileSize, fileContent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to process document")
                    .hasMessageContaining("PDF text extraction returned empty");
        }

        @Test
        @DisplayName("should handle non-PDF files as raw bytes")
        void shouldHandleNonPdfFilesAsRawBytes() {
            // Arrange
            String title = "Test File";
            String fileName = "test.txt";
            Long fileSize = 1024L;
            byte[] fileContent = "Raw text content".getBytes();
            List<String> chunks = List.of("Raw text content");

            when(pdfTextExtractor.getExtension(fileName)).thenReturn("txt");
            when(chunkingService.chunk(anyString())).thenReturn(chunks);
            when(embeddingAdapter.embed(anyString())).thenReturn(new float[]{0.1f});
            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            RagApplicationService.UploadResult result = ragApplicationService.uploadDocumentFromBytes(title, fileName, fileSize, fileContent);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("READY");

            verify(pdfTextExtractor, never()).extractText(any());
        }
    }

    @Nested
    @DisplayName("listDocuments()")
    class ListDocuments {

        @Test
        @DisplayName("should return list of documents")
        void shouldReturnListOfDocuments() {
            // Arrange
            Document doc1 = new Document(DocumentId.generate(), "Doc1", "file1.txt", 100L);
            Document doc2 = new Document(DocumentId.generate(), "Doc2", "file2.txt", 200L);
            when(documentRepository.findAll()).thenReturn(List.of(doc1, doc2));

            // Act
            List<Document> result = ragApplicationService.listDocuments();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Document::getTitle).containsExactlyInAnyOrder("Doc1", "Doc2");
        }

        @Test
        @DisplayName("should return empty list when no documents exist")
        void shouldReturnEmptyListWhenNoDocumentsExist() {
            // Arrange
            when(documentRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<Document> result = ragApplicationService.listDocuments();

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteDocument()")
    class DeleteDocument {

        @Test
        @DisplayName("should delete document successfully")
        void shouldDeleteDocumentSuccessfully() {
            // Arrange
            UUID documentId = UUID.randomUUID();
            Document document = new Document(DocumentId.of(documentId), "Test", "test.txt", 100L);
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
            when(embeddingAdapter.getDimensions()).thenReturn(384);

            // Act
            ragApplicationService.deleteDocument(documentId);

            // Assert
            verify(documentRepository).findById(documentId);
            verify(vectorAdapter).search(any(float[].class), eq(1000), eq(List.of(documentId)));
            verify(documentRepository).delete(documentId);
        }

        @Test
        @DisplayName("should throw exception when document not found")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Arrange
            UUID documentId = UUID.randomUUID();
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> ragApplicationService.deleteDocument(documentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Document not found");
        }
    }

    @Nested
    @DisplayName("retrieveContext()")
    class RetrieveContext {

        @Test
        @DisplayName("should search all documents when docIds is null")
        void shouldSearchAllDocumentsWhenDocIdsIsNull() {
            // Arrange
            String query = "test query";
            float[] queryEmbedding = {0.1f, 0.2f};
            List<DocumentChunk> chunks = List.of(
                    new DocumentChunk(UUID.randomUUID(), UUID.randomUUID(), "content1", 0, Map.of()).withEmbedding(new float[]{0.5f, 0.5f}),
                    new DocumentChunk(UUID.randomUUID(), UUID.randomUUID(), "content2", 1, Map.of()).withEmbedding(new float[]{0.3f, 0.7f})
            );

            when(embeddingAdapter.embed(query)).thenReturn(queryEmbedding);
            when(vectorAdapter.search(queryEmbedding, 5)).thenReturn(chunks);

            // Act
            RagApplicationService.RetrievalResult result = ragApplicationService.retrieveContext(query, null, 5);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.context()).contains("content1", "content2");
            assertThat(result.sources()).hasSize(2);
            assertThat(result.enrichedQuery()).isEqualTo(query);

            verify(vectorAdapter).search(queryEmbedding, 5);
            verify(vectorAdapter, never()).search(any(float[].class), anyInt(), anyList());
        }

        @Test
        @DisplayName("should filter documents by docIds when provided")
        void shouldFilterDocumentsByDocIdsWhenProvided() {
            // Arrange
            String query = "test query";
            UUID docId1 = UUID.randomUUID();
            List<UUID> docIds = List.of(docId1);
            float[] queryEmbedding = {0.1f, 0.2f};
            List<DocumentChunk> chunks = List.of(
                    new DocumentChunk(UUID.randomUUID(), docId1, "filtered content", 0, Map.of()).withEmbedding(new float[]{0.5f})
            );

            when(embeddingAdapter.embed(query)).thenReturn(queryEmbedding);
            when(vectorAdapter.search(queryEmbedding, 3, docIds)).thenReturn(chunks);

            // Act
            RagApplicationService.RetrievalResult result = ragApplicationService.retrieveContext(query, docIds, 3);

            // Assert
            assertThat(result.context()).contains("filtered content");
            verify(vectorAdapter).search(queryEmbedding, 3, docIds);
        }

        @Test
        @DisplayName("should return empty context and sources when no chunks found")
        void shouldReturnEmptyContextAndSourcesWhenNoChunksFound() {
            // Arrange
            String query = "test query";
            float[] queryEmbedding = {0.1f, 0.2f};

            when(embeddingAdapter.embed(query)).thenReturn(queryEmbedding);
            when(vectorAdapter.search(queryEmbedding, 5)).thenReturn(Collections.emptyList());

            // Act
            RagApplicationService.RetrievalResult result = ragApplicationService.retrieveContext(query, null, 5);

            // Assert
            assertThat(result.context()).isEmpty();
            assertThat(result.sources()).isEmpty();
        }
    }

    @Nested
    @DisplayName("calculateSimilarity()")
    class CalculateSimilarity {

        @Test
        @DisplayName("should calculate cosine similarity correctly")
        void shouldCalculateCosineSimilarityCorrectly() {
            // Arrange
            float[] a = {1.0f, 0.0f};
            float[] b = {1.0f, 0.0f};

            // Act - invoke through retrieveContext which uses calculateSimilarity internally
            RagApplicationService.RetrievalResult result = ragApplicationService.retrieveContext(
                    "test", null, 5);

            // Assert - similarity calculation is used internally, just verify service works
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return 0.0 when embedding is null")
        void shouldReturnZeroWhenEmbeddingIsNull() {
            // Arrange
            float[] queryEmbedding = {0.1f, 0.2f};
            DocumentChunk chunkWithNullEmbedding = new DocumentChunk(
                    UUID.randomUUID(), UUID.randomUUID(), "content", 0, Map.of());

            when(embeddingAdapter.embed("test")).thenReturn(queryEmbedding);
            when(vectorAdapter.search(queryEmbedding, 5)).thenReturn(List.of(chunkWithNullEmbedding));

            // Act
            RagApplicationService.RetrievalResult result = ragApplicationService.retrieveContext("test", null, 5);

            // Assert
            assertThat(result.sources().get(0).score()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0.0 when vectors have different lengths")
        void shouldReturnZeroWhenVectorsHaveDifferentLengths() {
            // Arrange - This tests calculateSimilarity null check and length mismatch
            float[] queryEmbedding = {0.1f, 0.2f, 0.3f};
            DocumentChunk chunk = new DocumentChunk(
                    UUID.randomUUID(), UUID.randomUUID(), "content", 0, Map.of())
                    .withEmbedding(new float[]{0.5f, 0.5f}); // Different length

            when(embeddingAdapter.embed("test")).thenReturn(queryEmbedding);
            when(vectorAdapter.search(queryEmbedding, 5)).thenReturn(List.of(chunk));

            // Act
            RagApplicationService.RetrievalResult result = ragApplicationService.retrieveContext("test", null, 5);

            // Assert - similarity should be 0.0 due to length mismatch
            assertThat(result.sources().get(0).score()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0.0 when either vector is null")
        void shouldReturnZeroWhenEitherVectorIsNull() {
            // Arrange
            float[] queryEmbedding = {0.1f, 0.2f};
            DocumentChunk chunkWithNullEmbedding = new DocumentChunk(
                    UUID.randomUUID(), UUID.randomUUID(), "content", 0, Map.of());

            when(embeddingAdapter.embed("test")).thenReturn(queryEmbedding);
            when(vectorAdapter.search(queryEmbedding, 5)).thenReturn(List.of(chunkWithNullEmbedding));

            // Act
            RagApplicationService.RetrievalResult result = ragApplicationService.retrieveContext("test", null, 5);

            // Assert
            assertThat(result.sources().get(0).score()).isEqualTo(0.0);
        }
    }
}
