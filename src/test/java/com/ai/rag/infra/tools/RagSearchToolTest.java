package com.ai.rag.infra.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.account.controller.OwnerContext;
import com.ai.common.infra.llm.ToolEventChannel;
import com.ai.rag.domain.model.RagDocument;
import com.ai.rag.domain.model.SourceCitation;
import com.ai.rag.service.RagApplicationService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagSearchTool")
class RagSearchToolTest {
  private static final String CHANNEL = "rag-search-tool-test";
  private static final String OWNER_KEY = "c:test-owner";

  @Mock private OwnerContext ownerContext;

  @Mock private RagApplicationService ragApplicationService;

  private RagSearchTool ragSearchTool;

  private static final String TEST_DOC_ID = "550e8400-e29b-41d4-a716-446655440000";
  private static final String TEST_DOC_TITLE = "Test Document";

  @BeforeEach
  void setUp() {
    ragSearchTool = new RagSearchTool(ragApplicationService, ownerContext);
    ToolEventChannel.open(CHANNEL);
    ToolEventChannel.bindOwnerKey(CHANNEL, OWNER_KEY);
  }

  @AfterEach
  void closeChannel() {
    ToolEventChannel.close(CHANNEL);
  }

  @Nested
  @DisplayName("searchDocuments")
  class SearchDocuments {

    @Test
    @DisplayName("should return search results with sources")
    void shouldReturnSearchResultsWithSources() {
      List<SourceCitation> sources =
          List.of(
              new SourceCitation("Test content 1", 0.95, Map.of("title", TEST_DOC_TITLE)),
              new SourceCitation("Test content 2", 0.85, Map.of("title", TEST_DOC_TITLE)));
      RagApplicationService.RetrievalResult retrievalResult =
          new RagApplicationService.RetrievalResult("context", sources, "query");
      when(ragApplicationService.retrieveContext(eq("test query"), isNull(), eq(5), eq(OWNER_KEY)))
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
      List<SourceCitation> sources =
          List.of(new SourceCitation("Test content", 0.95, Map.of("title", TEST_DOC_TITLE)));
      RagApplicationService.RetrievalResult retrievalResult =
          new RagApplicationService.RetrievalResult("context", sources, "query");
      when(ragApplicationService.retrieveContext(eq("test query"), anyList(), eq(5), eq(OWNER_KEY)))
          .thenReturn(retrievalResult);

      String result = ragSearchTool.searchDocuments("test query", List.of(TEST_DOC_ID));

      assertThat(result).contains("找到以下相关文档片段");
    }

    @Test
    @DisplayName("should return message when no results found")
    void shouldReturnMessageWhenNoResultsFound() {
      RagApplicationService.RetrievalResult retrievalResult =
          new RagApplicationService.RetrievalResult("", Collections.emptyList(), "query");
      when(ragApplicationService.retrieveContext(anyString(), any(), anyInt(), anyString()))
          .thenReturn(retrievalResult);

      String result = ragSearchTool.searchDocuments("nonexistent", null);

      assertThat(result).contains("没有找到与您查询相关的文档内容");
    }

    @Test
    @DisplayName("should return message for blank query")
    void shouldReturnMessageForBlankQuery() {
      String result = ragSearchTool.searchDocuments("  ", null);

      assertThat(result).isEqualTo("请提供有效的搜索查询");
      verifyNoInteractions(ragApplicationService);
    }

    @Test
    @DisplayName("should return message for null query")
    void shouldReturnMessageForNullQuery() {
      String result = ragSearchTool.searchDocuments(null, null);

      assertThat(result).isEqualTo("请提供有效的搜索查询");
      verifyNoInteractions(ragApplicationService);
    }

    @Test
    @DisplayName("should truncate long content")
    void shouldTruncateLongContent() {
      String longContent = "A".repeat(600);
      List<SourceCitation> sources =
          List.of(new SourceCitation(longContent, 0.95, Map.of("title", TEST_DOC_TITLE)));
      RagApplicationService.RetrievalResult retrievalResult =
          new RagApplicationService.RetrievalResult("context", sources, "query");
      when(ragApplicationService.retrieveContext(anyString(), any(), anyInt(), anyString()))
          .thenReturn(retrievalResult);

      String result = ragSearchTool.searchDocuments("test", null);

      assertThat(result).contains("...");
      assertThat(result.length()).isLessThan(1000);
    }

    @Test
    @DisplayName("should refuse to search documents when owner is unknown")
    void shouldRefuseToSearchDocumentsWhenOwnerIsUnknown() {
      ToolEventChannel.close(CHANNEL);

      String result = ragSearchTool.searchDocuments("test", null);

      assertThat(result).contains("无法识别用户");
      verifyNoInteractions(ragApplicationService);
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
    @DisplayName("should list only searchable documents of the owner bound to the tool channel")
    void shouldListOnlySearchableDocumentsOfTheOwnerBoundToTheToolChannel() {
      when(ragApplicationService.listSearchableDocuments(OWNER_KEY))
          .thenReturn(Collections.emptyList());

      ragSearchTool.listDocuments();
    }

    @Test
    @DisplayName("should refuse to list documents when owner is unknown")
    void shouldRefuseToListDocumentsWhenOwnerIsUnknown() {
      ToolEventChannel.close(CHANNEL);

      String result = ragSearchTool.listDocuments();

      assertThat(result).contains("无法识别用户");
      verifyNoInteractions(ragApplicationService);
    }

    @Test
    @DisplayName("should return document list")
    void shouldReturnDocumentList() {
      RagDocument doc = RagDocument.startIngestion(TEST_DOC_TITLE, "test.pdf", 1024L, OWNER_KEY);
      doc.completeIngestion(1);
      when(ragApplicationService.listSearchableDocuments(anyString())).thenReturn(List.of(doc));

      String result = ragSearchTool.listDocuments();

      assertThat(result).contains("知识库中的文档列表");
      assertThat(result).contains(doc.getId().toString());
      assertThat(result).contains(TEST_DOC_TITLE);
    }

    @Test
    @DisplayName("should return message when no documents")
    void shouldReturnMessageWhenNoDocuments() {
      when(ragApplicationService.listSearchableDocuments(anyString()))
          .thenReturn(Collections.emptyList());

      String result = ragSearchTool.listDocuments();

      assertThat(result).isEqualTo("知识库中暂无文档，请先上传文档。");
    }

    @Test
    @DisplayName("should handle service exception")
    void shouldHandleServiceException() {
      when(ragApplicationService.listSearchableDocuments(anyString()))
          .thenThrow(new RuntimeException("Service error"));

      String result = ragSearchTool.listDocuments();

      assertThat(result).contains("获取文档列表时发生未知错误");
    }
  }
}
