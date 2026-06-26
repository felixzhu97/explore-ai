package com.ai.rag.application.usecase;

import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.SourceDocument;
import com.ai.rag.domain.vo.DocumentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagApplicationService")
class RagApplicationServiceTest {

    @Mock
    private DocumentUploadService uploadService;

    @Mock
    private DocumentSearchService searchService;

    private RagApplicationService service;

    @BeforeEach
    void setUp() {
        service = new RagApplicationService(uploadService, searchService);
    }

    @Nested
    @DisplayName("uploadDocument()")
    class UploadDocument {

        @Test
        @DisplayName("should delegate to uploadService")
        void shouldDelegateToUploadService() {
            DocumentId docId = DocumentId.generate();
            var uploadResult = new DocumentUploadService.UploadResult(docId, "Test", "READY", 3);
            when(uploadService.upload("Test", "file.txt", 1024L, "content")).thenReturn(uploadResult);

            var result = service.uploadDocument("Test", "file.txt", 1024L, "content");

            assertThat(result.documentId()).isEqualTo(docId);
            assertThat(result.title()).isEqualTo("Test");
            assertThat(result.status()).isEqualTo("READY");
            assertThat(result.chunkCount()).isEqualTo(3);
            verify(uploadService).upload("Test", "file.txt", 1024L, "content");
        }
    }

    @Nested
    @DisplayName("listDocuments()")
    class ListDocuments {

        @Test
        @DisplayName("should delegate to uploadService")
        void shouldDelegateToUploadService() {
            Document doc1 = new Document(DocumentId.generate(), "Doc1", "file1.txt", 100L);
            Document doc2 = new Document(DocumentId.generate(), "Doc2", "file2.txt", 200L);
            when(uploadService.listAll()).thenReturn(List.of(doc1, doc2));

            List<Document> result = service.listDocuments();

            assertThat(result).hasSize(2);
            verify(uploadService).listAll();
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void shouldReturnEmptyListWhenNoDocuments() {
            when(uploadService.listAll()).thenReturn(List.of());

            List<Document> result = service.listDocuments();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteDocument()")
    class DeleteDocument {

        @Test
        @DisplayName("should delegate to uploadService")
        void shouldDelegateToUploadService() {
            UUID documentId = UUID.randomUUID();

            service.deleteDocument(documentId);

            verify(uploadService).delete(documentId);
        }
    }

    @Nested
    @DisplayName("retrieveContext()")
    class RetrieveContext {

        @Test
        @DisplayName("should delegate to searchService and wrap result")
        void shouldDelegateToSearchServiceAndWrapResult() {
            String query = "test query";
            var searchResult = new DocumentSearchService.RetrievalResult(
                    "context content",
                    List.of(new SourceDocument("source text", 0.95, Map.of())));
            when(searchService.retrieve(query, null, 5)).thenReturn(searchResult);

            RagApplicationService.RetrievalResult result = service.retrieveContext(query, null, 5);

            assertThat(result.context()).isEqualTo("context content");
            assertThat(result.sources()).hasSize(1);
            assertThat(result.enrichedQuery()).isEqualTo(query);
            verify(searchService).retrieve(query, null, 5);
        }

        @Test
        @DisplayName("should pass docIds to searchService")
        void shouldPassDocIdsToSearchService() {
            String query = "test";
            DocumentId docId = DocumentId.generate();
            var searchResult = new DocumentSearchService.RetrievalResult("ctx", List.of());
            when(searchService.retrieve(query, List.of(docId), 3)).thenReturn(searchResult);

            service.retrieveContext(query, List.of(docId), 3);

            verify(searchService).retrieve(query, List.of(docId), 3);
        }
    }
}
