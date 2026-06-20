package com.ai.modules.rag.infrastructure.tools;

import com.ai.modules.rag.application.usecase.RagApplicationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG Search tool for AI function calling.
 * Allows AI to search documents in the knowledge base.
 */
@Component
public class RagSearchTool {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_CONTENT_LENGTH = 500;

    private final RagApplicationService ragApplicationService;

    public RagSearchTool(RagApplicationService ragApplicationService) {
        this.ragApplicationService = ragApplicationService;
    }

    @Tool(description = "搜索知识库中的文档内容。根据用户问题搜索相关文档片段并返回匹配的内容和来源信息")
    public String searchDocuments(
            @ToolParam(description = "搜索查询，用于在文档中查找相关内容") String query,
            @ToolParam(description = "要搜索的文档ID列表（可选，不提供则搜索所有文档）", required = false) List<String> docIds
    ) {
        if (query == null || query.isBlank()) {
            return "请提供有效的搜索查询";
        }

        try {
            List<UUID> docUuids = null;
            if (docIds != null && !docIds.isEmpty()) {
                docUuids = docIds.stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toList());
            }

            var result = ragApplicationService.retrieveContext(query, docUuids, DEFAULT_TOP_K);

            if (result.sources().isEmpty()) {
                return "没有找到与您查询相关的文档内容。请尝试不同的搜索关键词。";
            }

            StringBuilder response = new StringBuilder();
            response.append("找到以下相关文档片段：\n\n");

            for (int i = 0; i < result.sources().size(); i++) {
                var source = result.sources().get(i);
                String content = source.text();
                if (content.length() > MAX_CONTENT_LENGTH) {
                    content = content.substring(0, MAX_CONTENT_LENGTH) + "...";
                }

                response.append(String.format("【来源 %d】相似度: %.2f\n", i + 1, source.score()));
                response.append(content);
                response.append("\n\n");

                Object title = source.metadata().get("title");
                if (title != null) {
                    response.append(String.format("文档: %s\n", title));
                }
                response.append("---\n\n");
            }

            return response.toString();

        } catch (IllegalArgumentException e) {
            return "文档ID格式无效，请提供有效的UUID格式的文档ID。";
        } catch (Exception e) {
            return "搜索文档时发生错误：" + e.getMessage();
        }
    }

    @Tool(description = "列出知识库中的所有可用文档，返回文档ID和标题")
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
            return "获取文档列表时发生错误：" + e.getMessage();
        }
    }
}
