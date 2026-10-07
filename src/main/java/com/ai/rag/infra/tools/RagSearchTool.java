package com.ai.rag.infra.tools;

import com.ai.account.controller.OwnerContext;
import com.ai.common.infra.llm.ToolEventChannel;
import com.ai.common.service.llm.DocumentSearchTool;
import com.ai.rag.domain.model.DocumentId;
import com.ai.rag.infra.vector.ChunkMetadataKeys;
import com.ai.rag.service.RagApplicationService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** LLM tools that search the knowledge base and list the current owner's documents. */
@Component
@RequiredArgsConstructor
public class RagSearchTool implements DocumentSearchTool {

  private static final int DEFAULT_TOP_K = 5;
  private static final int MAX_CONTENT_LENGTH = 500;
  private static final String UNKNOWN_OWNER_MESSAGE = "当前上下文无法识别用户，暂时无法访问文档。";

  private final RagApplicationService ragApplicationService;
  private final OwnerContext ownerContext;

  @Override
  @Tool(
      name = "search_documents",
      description = "Search documents in the knowledge base for relevant information")
  public String searchDocuments(
      @ToolParam(description = "The search query text") String query,
      @ToolParam(description = "Optional list of document IDs to filter", required = false)
          List<String> documentIds) {
    if (query == null || query.isBlank()) {
      return "请提供有效的搜索查询";
    }
    Optional<String> ownerKey = getCurrentOwnerKey();
    if (ownerKey.isEmpty()) {
      return UNKNOWN_OWNER_MESSAGE;
    }

    try {
      List<DocumentId> documentIdList =
          documentIds != null && !documentIds.isEmpty()
              ? documentIds.stream().map(DocumentId::parseId).collect(Collectors.toList())
              : null;

      var result =
          ragApplicationService.retrieveContext(
              query, documentIdList, DEFAULT_TOP_K, ownerKey.get());

      if (result.sources().isEmpty()) {
        return "没有找到与您查询相关的文档内容。请尝试不同的搜索关键词。";
      }

      return "找到以下相关文档片段：\n\n" + formatSources(result.sources());
    } catch (IllegalArgumentException e) {
      return "文档ID格式无效，请提供有效的UUID格式的文档ID。";
    } catch (Exception e) {
      return "搜索文档时发生未知错误，请稍后重试。";
    }
  }

  @Override
  @Tool(name = "list_documents", description = "List all documents in the knowledge base")
  public String listDocuments() {
    Optional<String> ownerKey = getCurrentOwnerKey();
    if (ownerKey.isEmpty()) {
      return UNKNOWN_OWNER_MESSAGE;
    }
    try {
      var documents = ragApplicationService.listSearchableDocuments(ownerKey.get());

      if (documents.isEmpty()) {
        return "知识库中暂无文档，请先上传文档。";
      }

      StringBuilder response = new StringBuilder();
      response.append("知识库中的文档列表：\n\n");

      for (var doc : documents) {
        response.append(String.format("- ID: %s\n", doc.getId().getValue()));
        response.append(String.format("  标题: %s\n", doc.getTitle()));
        response.append(String.format("  状态: %s\n", doc.getStatus()));
        response.append(String.format("  创建时间: %s\n\n", doc.getCreatedAt()));
      }

      return response.toString();

    } catch (Exception e) {
      return "获取文档列表时发生未知错误，请稍后重试。";
    }
  }

  private Optional<String> getCurrentOwnerKey() {
    Optional<String> bound = ToolEventChannel.getCurrentOwnerKey();
    if (bound.isPresent()) {
      return bound;
    }
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
      return Optional.of(ownerContext.requireValue(attrs.getRequest()));
    }
    return Optional.empty();
  }

  private String formatSources(List<com.ai.rag.domain.model.SourceCitation> sources) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < sources.size(); i++) {
      var source = sources.get(i);
      String content = excerpt(source.getContent());
      sb.append(String.format("【来源 %d】相似度: %.2f\n%s\n", i + 1, source.getScore(), content));
      if (source.getMetadata() != null
          && source.getMetadata().get(ChunkMetadataKeys.TITLE) instanceof String title) {
        sb.append(String.format("文档: %s\n", title));
      }
      sb.append("---\n\n");
    }
    return sb.toString();
  }

  private static String excerpt(String content) {
    if (content == null || content.length() <= MAX_CONTENT_LENGTH) {
      return content;
    }
    return content.substring(0, MAX_CONTENT_LENGTH) + "...";
  }
}
