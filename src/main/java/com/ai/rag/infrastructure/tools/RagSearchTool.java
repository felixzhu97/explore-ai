package com.ai.rag.infrastructure.tools;

import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.rag.application.usecase.RagApplicationService;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.common.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RagSearchTool implements DocumentSearchTool {

    private static final Logger log = LoggerFactory.getLogger(RagSearchTool.class);
    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_CONTENT_LENGTH = 500;

    private final RagApplicationService ragApplicationService;

    public RagSearchTool(RagApplicationService ragApplicationService) {
        this.ragApplicationService = ragApplicationService;
    }

    @Override
    @Tool(name = "search_documents", description = "Search documents in the knowledge base for relevant information")
    public String searchDocuments(
            @ToolParam(description = "The search query text") String query,
            @ToolParam(description = "Optional list of document IDs to filter", required = false) List<String> docIds) {
        if (query == null || query.isBlank()) {
            return "请提供有效的搜索查询";
        }

        try {
            List<DocumentId> docIdList = docIds != null && !docIds.isEmpty()
                    ? docIds.stream().map(DocumentId::of).collect(Collectors.toList())
                    : null;

            var result = ragApplicationService.retrieveContext(query, docIdList, DEFAULT_TOP_K);

            if (result.sources().isEmpty()) {
                return "没有找到与您查询相关的文档内容。请尝试不同的搜索关键词。";
            }

            return "找到以下相关文档片段：\n\n" + formatSources(result.sources());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid document ID format in searchDocuments", e);
            return "文档ID格式无效，请提供有效的UUID格式的文档ID。";
        } catch (Exception e) {
            log.error("Error searching documents", e);
            return "搜索文档时发生未知错误，请稍后重试。";
        }
    }

    @Override
    @Tool(name = "list_documents", description = "List all documents in the knowledge base")
    public String listDocuments() {
        try {
            var documents = ragApplicationService.listDocuments();

            if (documents.isEmpty()) {
                return "知识库中暂无文档，请先上传文档。";
            }

            StringBuilder response = new StringBuilder();
            response.append("知识库中的文档列表：\n\n");

            for (var doc : documents) {
                response.append(String.format("- ID: %s\n", doc.getId().value()));
                response.append(String.format("  标题: %s\n", doc.getTitle()));
                response.append(String.format("  状态: %s\n", doc.getStatus()));
                response.append(String.format("  创建时间: %s\n\n", doc.getCreatedAt()));
            }

            return response.toString();

        } catch (Exception e) {
            log.error("Error listing documents", e);
            return "获取文档列表时发生未知错误，请稍后重试。";
        }
    }

    private String formatSources(List<com.ai.rag.domain.model.SourceDocument> sources) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            var source = sources.get(i);
            String content = LogSanitizer.truncate(source.text(), MAX_CONTENT_LENGTH);
            sb.append(String.format("【来源 %d】相似度: %.2f\n%s\n", i + 1, source.score(), content));
            if (source.metadata() != null && source.metadata().get("title") instanceof String title) {
                sb.append(String.format("文档: %s\n", title));
            }
            sb.append("---\n\n");
        }
        return sb.toString();
    }

}
