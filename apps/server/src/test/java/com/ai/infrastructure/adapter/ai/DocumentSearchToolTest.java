package com.ai.infrastructure.adapter.ai;

import com.ai.application.service.RagApplicationService;
import com.ai.application.usecase.RagChatUseCase.RetrievalResult;
import com.ai.domain.model.SourceDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DocumentSearchTool Unit Tests
 *
 * Tests the document search tool for RAG system:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests searchDocuments and searchDocumentsInIds methods
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentSearchTool")
class DocumentSearchToolTest {

    @Mock
    private RagApplicationService ragApplicationService;

    private DocumentSearchTool documentSearchTool;

    @BeforeEach
    void setUp() {
        documentSearchTool = new DocumentSearchTool(ragApplicationService);
    }

    @Nested
    @DisplayName("searchDocuments")
    class SearchDocumentsTests {

        @Test
        @DisplayName("should return formatted search results when successful")
        void shouldReturnFormattedSearchResultsWhenSuccessful() {
            // Arrange
            String query = "test query";
            List<SourceDocument> sources = List.of(
                new SourceDocument(1, "Document 1 content", 0.95, "doc1", Map.of()),
                new SourceDocument(2, "Document 2 content", 0.85, "doc2", Map.of())
            );
            RetrievalResult result = new RetrievalResult("context", sources, "enriched query");
            when(ragApplicationService.retrieveContext(eq(query), isNull(), eq(5))).thenReturn(result);

            // Act
            String searchResult = documentSearchTool.searchDocuments(query, null);

            // Assert
            assertThat(searchResult).contains("2 relevant documents");
            assertThat(searchResult).contains("Document 1 content");
            assertThat(searchResult).contains("Document 2 content");
            assertThat(searchResult).contains("0.95");
            assertThat(searchResult).contains("0.85");
            verify(ragApplicationService).retrieveContext(query, null, 5);
        }

        @Test
        @DisplayName("should use custom maxResults when provided")
        void shouldUseCustomMaxResultsWhenProvided() {
            // Arrange
            String query = "test query";
            Integer maxResults = 10;
            RetrievalResult result = new RetrievalResult("context", List.of(), "enriched query");
            when(ragApplicationService.retrieveContext(eq(query), isNull(), eq(10))).thenReturn(result);

            // Act
            documentSearchTool.searchDocuments(query, maxResults);

            // Assert
            verify(ragApplicationService).retrieveContext(query, null, 10);
        }

        @Test
        @DisplayName("should use default maxResults of 5 when null")
        void shouldUseDefaultMaxResultsOf5WhenNull() {
            // Arrange
            String query = "test query";
            RetrievalResult result = new RetrievalResult("context", List.of(), "enriched query");
            when(ragApplicationService.retrieveContext(eq(query), isNull(), eq(5))).thenReturn(result);

            // Act
            documentSearchTool.searchDocuments(query, null);

            // Assert
            verify(ragApplicationService).retrieveContext(query, null, 5);
        }

        @Test
        @DisplayName("should handle empty results")
        void shouldHandleEmptyResults() {
            // Arrange
            String query = "no results query";
            RetrievalResult result = new RetrievalResult("context", List.of(), "enriched query");
            when(ragApplicationService.retrieveContext(any(), any(), anyInt())).thenReturn(result);

            // Act
            String searchResult = documentSearchTool.searchDocuments(query, null);

            // Assert
            assertThat(searchResult).contains("0 relevant documents");
        }

        @Test
        @DisplayName("should handle single result")
        void shouldHandleSingleResult() {
            // Arrange
            String query = "single result query";
            SourceDocument singleDoc = new SourceDocument(1, "Single document content", 0.99, "single", Map.of());
            RetrievalResult result = new RetrievalResult("context", List.of(singleDoc), "enriched query");
            when(ragApplicationService.retrieveContext(any(), any(), anyInt())).thenReturn(result);

            // Act
            String searchResult = documentSearchTool.searchDocuments(query, null);

            // Assert
            assertThat(searchResult).contains("1 relevant document");
            assertThat(searchResult).contains("Single document content");
        }

        @Test
        @DisplayName("should format relevance scores with 2 decimal places")
        void shouldFormatRelevanceScoresWith2DecimalPlaces() {
            // Arrange
            String query = "test query";
            SourceDocument doc = new SourceDocument(1, "content", 0.123456789, "test", Map.of());
            RetrievalResult result = new RetrievalResult("context", List.of(doc), "enriched query");
            when(ragApplicationService.retrieveContext(any(), any(), anyInt())).thenReturn(result);

            // Act
            String searchResult = documentSearchTool.searchDocuments(query, null);

            // Assert
            assertThat(searchResult).contains("0.12");
        }

        @Test
        @DisplayName("should return error message when exception occurs")
        void shouldReturnErrorMessageWhenExceptionOccurs() {
            // Arrange
            String query = "error query";
            when(ragApplicationService.retrieveContext(any(), any(), anyInt()))
                    .thenThrow(new RuntimeException("Search failed"));

            // Act
            String searchResult = documentSearchTool.searchDocuments(query, null);

            // Assert
            assertThat(searchResult).startsWith("Error searching documents:");
            assertThat(searchResult).contains("Search failed");
        }

        @Test
        @DisplayName("should handle exception with null message")
        void shouldHandleExceptionWithNullMessage() {
            // Arrange
            String query = "error query";
            when(ragApplicationService.retrieveContext(any(), any(), anyInt()))
                    .thenThrow(new RuntimeException());

            // Act
            String searchResult = documentSearchTool.searchDocuments(query, null);

            // Assert
            assertThat(searchResult).startsWith("Error searching documents:");
        }
    }

    @Nested
    @DisplayName("searchDocumentsInIds")
    class SearchDocumentsInIdsTests {

        @Test
        @DisplayName("should return formatted search results when successful with IDs")
        void shouldReturnFormattedSearchResultsWhenSuccessfulWithIds() {
            // Arrange
            String query = "test query";
            String docIds = "550e8400-e29b-41d4-a716-446655440000,550e8400-e29b-41d4-a716-446655440001";
            List<SourceDocument> sources = List.of(
                new SourceDocument(1, "Found document", 0.92, "found", Map.of("docId", "550e8400-e29b-41d4-a716-446655440000"))
            );
            RetrievalResult result = new RetrievalResult("context", sources, "enriched query");
            when(ragApplicationService.retrieveContext(eq(query), any(), eq(5))).thenReturn(result);

            // Act
            String searchResult = documentSearchTool.searchDocumentsInIds(query, docIds, null);

            // Assert
            assertThat(searchResult).contains("1 relevant document");
            assertThat(searchResult).contains("Found document");
            verify(ragApplicationService).retrieveContext(eq(query), argThat(list ->
                list.size() == 2 &&
                list.get(0).equals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")) &&
                list.get(1).equals(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"))
            ), eq(5));
        }

        @Test
        @DisplayName("should handle docIds with spaces")
        void shouldHandleDocIdsWithSpaces() {
            // Arrange
            String query = "test query";
            String docIds = "550e8400-e29b-41d4-a716-446655440000, 550e8400-e29b-41d4-a716-446655440001";
            RetrievalResult result = new RetrievalResult("context", List.of(), "enriched query");
            when(ragApplicationService.retrieveContext(any(), any(), anyInt())).thenReturn(result);

            // Act
            documentSearchTool.searchDocumentsInIds(query, docIds, null);

            // Assert - should not throw and should trim spaces
            verify(ragApplicationService).retrieveContext(eq(query), argThat(list ->
                list.size() == 2
            ), anyInt());
        }

        @Test
        @DisplayName("should handle single document ID")
        void shouldHandleSingleDocumentId() {
            // Arrange
            String query = "test query";
            String docId = "550e8400-e29b-41d4-a716-446655440000";
            RetrievalResult result = new RetrievalResult("context", List.of(), "enriched query");
            when(ragApplicationService.retrieveContext(any(), any(), anyInt())).thenReturn(result);

            // Act
            documentSearchTool.searchDocumentsInIds(query, docId, null);

            // Assert
            verify(ragApplicationService).retrieveContext(eq(query), argThat(list ->
                list.size() == 1
            ), anyInt());
        }

        @Test
        @DisplayName("should use custom maxResults when provided")
        void shouldUseCustomMaxResultsWhenProvided() {
            // Arrange
            String query = "test query";
            String docIds = "550e8400-e29b-41d4-a716-446655440000";
            Integer maxResults = 3;
            RetrievalResult result = new RetrievalResult("context", List.of(), "enriched query");
            when(ragApplicationService.retrieveContext(any(), any(), eq(3))).thenReturn(result);

            // Act
            documentSearchTool.searchDocumentsInIds(query, docIds, maxResults);

            // Assert
            verify(ragApplicationService).retrieveContext(any(), any(), eq(3));
        }

        @Test
        @DisplayName("should handle empty results with IDs")
        void shouldHandleEmptyResultsWithIds() {
            // Arrange
            String query = "no results query";
            String docIds = "550e8400-e29b-41d4-a716-446655440000";
            RetrievalResult result = new RetrievalResult("context", List.of(), "enriched query");
            when(ragApplicationService.retrieveContext(any(), any(), anyInt())).thenReturn(result);

            // Act
            String searchResult = documentSearchTool.searchDocumentsInIds(query, docIds, null);

            // Assert
            assertThat(searchResult).contains("0 relevant documents");
        }

        @Test
        @DisplayName("should return error message when exception occurs with IDs")
        void shouldReturnErrorMessageWhenExceptionOccursWithIds() {
            // Arrange
            String query = "error query";
            String docIds = "550e8400-e29b-41d4-a716-446655440000";
            when(ragApplicationService.retrieveContext(any(), any(), anyInt()))
                    .thenThrow(new RuntimeException("Search with IDs failed"));

            // Act
            String searchResult = documentSearchTool.searchDocumentsInIds(query, docIds, null);

            // Assert
            assertThat(searchResult).startsWith("Error searching documents:");
            assertThat(searchResult).contains("Search with IDs failed");
        }

        @Test
        @DisplayName("should return error message when UUID parsing fails")
        void shouldReturnErrorMessageWhenUuidParsingFails() {
            // Arrange
            String query = "test query";
            String invalidDocIds = "invalid-uuid,not-a-uuid";

            // Act
            String searchResult = documentSearchTool.searchDocumentsInIds(query, invalidDocIds, null);

            // Assert
            assertThat(searchResult).startsWith("Error searching documents:");
        }
    }

    @Nested
    @DisplayName("Multiple Documents Formatting")
    class MultipleDocumentsFormattingTests {

        @Test
        @DisplayName("should format multiple documents with correct numbering")
        void shouldFormatMultipleDocumentsWithCorrectNumbering() {
            // Arrange
            String query = "multi doc query";
            List<SourceDocument> sources = List.of(
                new SourceDocument(1, "First content", 0.9, "first", Map.of()),
                new SourceDocument(2, "Second content", 0.8, "second", Map.of()),
                new SourceDocument(3, "Third content", 0.7, "third", Map.of())
            );
            RetrievalResult result = new RetrievalResult("context", sources, "enriched query");
            when(ragApplicationService.retrieveContext(any(), any(), anyInt())).thenReturn(result);

            // Act
            String searchResult = documentSearchTool.searchDocuments(query, null);

            // Assert
            assertThat(searchResult).contains("Document 1:");
            assertThat(searchResult).contains("Document 2:");
            assertThat(searchResult).contains("Document 3:");
            assertThat(searchResult).contains("First content");
            assertThat(searchResult).contains("Second content");
            assertThat(searchResult).contains("Third content");
        }

        @Test
        @DisplayName("should include metadata in document output")
        void shouldIncludeMetadataInDocumentOutput() {
            // Arrange
            String query = "metadata test";
            Map<String, Object> metadata = Map.of("source", "test.pdf", "page", 1);
            SourceDocument doc = new SourceDocument(1, "Content with metadata", 0.95, "test", metadata);
            RetrievalResult result = new RetrievalResult("context", List.of(doc), "enriched query");
            when(ragApplicationService.retrieveContext(any(), any(), anyInt())).thenReturn(result);

            // Act
            String searchResult = documentSearchTool.searchDocuments(query, null);

            // Assert
            assertThat(searchResult).contains("Content with metadata");
        }
    }
}
