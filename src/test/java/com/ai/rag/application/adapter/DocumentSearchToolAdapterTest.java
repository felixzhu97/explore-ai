package com.ai.rag.application.adapter;

import com.ai.rag.application.usecase.RagApplicationService;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentSearchToolAdapter")
class DocumentSearchToolAdapterTest {

    @Mock
    private RagApplicationService ragApplicationService;

    private DocumentSearchToolAdapter documentSearchToolAdapter;

    private static final String TEST_DOC_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_DOC_TITLE = "Test Document";

    @BeforeEach
    void setUp() {
        documentSearchToolAdapter = new DocumentSearchToolAdapter(ragApplicationService);
    }

    @Nested
    @DisplayName("searchDocuments")
    class SearchDocuments {

        @Test
        @DisplayName("should return search results with sources")
        void shouldReturnSearchResultsWithSources() {
            List<SourceDocument> sources = List.of(
                    new SourceDocument("Test content 1", 0.95, Map.of("title", TEST_DOC_TITLE)),
                    new SourceDocument("Test content 2", 0.85, Map.of("title", TEST_DOC_TITLE))
            );
            RagApplicationService.RetrievalResult retrievalResult = new RagApplicationService.RetrievalResult(
                    "context", sources, "query"
            );
            when(ragApplicationService.retrieveContext(eq("test query"), isNull(), eq(5)))
                    .thenReturn(retrievalResult);

            String result = documentSearchToolAdapter.searchDocuments("test query", null);

            assertThat(result).contains("找到以下相关文档片段");
            assertThat(result).contains("【来源 1】");
            assertThat(result).contains("【来源 2】");
            assertThat(result).contains(TEST_DOC_TITLE);
        }

        @Test
        @DisplayName("should search with specific document IDs")
        void shouldSearchWithSpecificDocIds() {
            List<SourceDocument> sources = List.of(
                    new SourceDocument("Test content", 0.95, Map.of("title", TEST_DOC_TITLE))
            );
            RagApplicationService.RetrievalResult retrievalResult = new RagApplicationService.RetrievalResult(
                    "context", sources, "query"
            );
            when(ragApplicationService.retrieveContext(eq("test query"), anyList(), eq(5)))
                    .thenReturn(retrievalResult);

            String result = documentSearchToolAdapter.searchDocuments("test query", List.of(TEST_DOC_ID));

            assertThat(result).contains("找到以下相关文档片段");
            verify(ragApplicationService).retrieveContext(eq("test query"), anyList(), eq(5));
        }

        @Test
        @DisplayName("should return message when no results found")
        void shouldReturnMessageWhenNoResultsFound() {
            RagApplicationService.RetrievalResult retrievalResult = new RagApplicationService.RetrievalResult(
                    "", Collections.emptyList(), "query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), anyInt()))
                    .thenReturn(retrievalResult);

            String result = documentSearchToolAdapter.searchDocuments("nonexistent", null);

            assertThat(result).contains("没有找到与您查询相关的文档内容");
        }

        @Test
        @DisplayName("should return message for blank query")
        void shouldReturnMessageForBlankQuery() {
            String result = documentSearchToolAdapter.searchDocuments("  ", null);

            assertThat(result).isEqualTo("请提供有效的搜索查询");
            verifyNoInteractions(ragApplicationService);
        }

        @Test
        @DisplayName("should return message for null query")
        void shouldReturnMessageForNullQuery() {
            String result = documentSearchToolAdapter.searchDocuments(null, null);

            assertThat(result).isEqualTo("请提供有效的搜索查询");
            verifyNoInteractions(ragApplicationService);
        }

        @Test
        @DisplayName("should truncate long content")
        void shouldTruncateLongContent() {
            String longContent = "A".repeat(600);
            List<SourceDocument> sources = List.of(
                    new SourceDocument(longContent, 0.95, Map.of("title", TEST_DOC_TITLE))
            );
            RagApplicationService.RetrievalResult retrievalResult = new RagApplicationService.RetrievalResult(
                    "context", sources, "query"
            );
            when(ragApplicationService.retrieveContext(anyString(), any(), anyInt()))
                    .thenReturn(retrievalResult);

            String result = documentSearchToolAdapter.searchDocuments("test", null);

            assertThat(result).contains("...");
            assertThat(result.length()).isLessThan(1000);
        }

        @Test
        @DisplayName("should handle invalid UUID format")
        void shouldHandleInvalidUuidFormat() {
            String result = documentSearchToolAdapter.searchDocuments("test", List.of("invalid-uuid"));

            assertThat(result).contains("文档ID格式无效");
        }
    }

    @Nested
    @DisplayName("listDocuments")
    class ListDocuments {

        @Test
        @DisplayName("should return document list")
        void shouldReturnDocumentList() {
            DocumentId docId = DocumentId.of(UUID.fromString(TEST_DOC_ID));
            Document doc = new Document(docId, TEST_DOC_TITLE, "test.pdf", 1024L);
            when(ragApplicationService.listDocuments()).thenReturn(List.of(doc));

            String result = documentSearchToolAdapter.listDocuments();

            assertThat(result).contains("知识库中的文档列表");
            assertThat(result).contains(TEST_DOC_ID);
            assertThat(result).contains(TEST_DOC_TITLE);
        }

        @Test
        @DisplayName("should return message when no documents")
        void shouldReturnMessageWhenNoDocuments() {
            when(ragApplicationService.listDocuments()).thenReturn(Collections.emptyList());

            String result = documentSearchToolAdapter.listDocuments();

            assertThat(result).isEqualTo("知识库中暂无文档，请先上传文档。");
        }

        @Test
        @DisplayName("should handle service exception")
        void shouldHandleServiceException() {
            when(ragApplicationService.listDocuments()).thenThrow(new RuntimeException("Service error"));

            String result = documentSearchToolAdapter.listDocuments();

            assertThat(result).contains("获取文档列表时发生未知错误");
        }
    }
}
