package com.ai.adapter.out.tools;

import com.ai.domain.model.Document;
import com.ai.domain.model.SourceDocument;
import com.ai.domain.service.RagService;
import com.ai.domain.vo.DocumentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagSearchTool")
class RagSearchToolTest {

    @Mock
    private RagService ragService;

    private RagSearchTool ragSearchTool;

    private static final String TEST_DOC_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_DOC_TITLE = "Test Document";

    @BeforeEach
    void setUp() {
        ragSearchTool = new RagSearchTool(ragService);
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
            RagService.RetrievalResult retrievalResult = new RagService.RetrievalResult(
                    "context", sources, "query"
            );
            when(ragService.retrieveContext(eq("test query"), isNull(), eq(5)))
                    .thenReturn(retrievalResult);

            String result = ragSearchTool.searchDocuments("test query", null);

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
            RagService.RetrievalResult retrievalResult = new RagService.RetrievalResult(
                    "context", sources, "query"
            );
            when(ragService.retrieveContext(eq("test query"), anyList(), eq(5)))
                    .thenReturn(retrievalResult);

            String result = ragSearchTool.searchDocuments("test query", List.of(TEST_DOC_ID));

            assertThat(result).contains("找到以下相关文档片段");
            verify(ragService).retrieveContext(eq("test query"), anyList(), eq(5));
        }

        @Test
        @DisplayName("should return message when no results found")
        void shouldReturnMessageWhenNoResultsFound() {
            RagService.RetrievalResult retrievalResult = new RagService.RetrievalResult(
                    "", Collections.emptyList(), "query"
            );
            when(ragService.retrieveContext(anyString(), any(), anyInt()))
                    .thenReturn(retrievalResult);

            String result = ragSearchTool.searchDocuments("nonexistent", null);

            assertThat(result).contains("没有找到与您查询相关的文档内容");
        }

        @Test
        @DisplayName("should return message for blank query")
        void shouldReturnMessageForBlankQuery() {
            String result = ragSearchTool.searchDocuments("  ", null);

            assertThat(result).isEqualTo("请提供有效的搜索查询");
            verifyNoInteractions(ragService);
        }

        @Test
        @DisplayName("should return message for null query")
        void shouldReturnMessageForNullQuery() {
            String result = ragSearchTool.searchDocuments(null, null);

            assertThat(result).isEqualTo("请提供有效的搜索查询");
            verifyNoInteractions(ragService);
        }

        @Test
        @DisplayName("should truncate long content")
        void shouldTruncateLongContent() {
            String longContent = "A".repeat(600);
            List<SourceDocument> sources = List.of(
                    new SourceDocument(longContent, 0.95, Map.of("title", TEST_DOC_TITLE))
            );
            RagService.RetrievalResult retrievalResult = new RagService.RetrievalResult(
                    "context", sources, "query"
            );
            when(ragService.retrieveContext(anyString(), any(), anyInt()))
                    .thenReturn(retrievalResult);

            String result = ragSearchTool.searchDocuments("test", null);

            assertThat(result).contains("...");
            assertThat(result.length()).isLessThan(1000);
        }

        @Test
        @DisplayName("should handle invalid UUID format")
        void shouldHandleInvalidUuidFormat() {
            String result = ragSearchTool.searchDocuments("test", List.of("invalid-uuid"));

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
            when(ragService.listDocuments()).thenReturn(List.of(doc));

            String result = ragSearchTool.listDocuments();

            assertThat(result).contains("知识库中的文档列表");
            assertThat(result).contains(TEST_DOC_ID);
            assertThat(result).contains(TEST_DOC_TITLE);
        }

        @Test
        @DisplayName("should return message when no documents")
        void shouldReturnMessageWhenNoDocuments() {
            when(ragService.listDocuments()).thenReturn(Collections.emptyList());

            String result = ragSearchTool.listDocuments();

            assertThat(result).isEqualTo("知识库中暂无文档，请先上传文档。");
        }

        @Test
        @DisplayName("should handle service exception")
        void shouldHandleServiceException() {
            when(ragService.listDocuments()).thenThrow(new RuntimeException("Service error"));

            String result = ragSearchTool.listDocuments();

            assertThat(result).contains("获取文档列表时发生错误");
            assertThat(result).contains("Service error");
        }
    }
}
