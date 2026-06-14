package com.ai.infrastructure.adapter.tool.rag;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.domain.model.Document;
import com.ai.infrastructure.adapter.tool.ToolProvider;
import com.ai.infrastructure.adapter.tool.JsonSchemaBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool for listing all documents.
 */
@Component
public class DocumentListTool implements ToolProvider {

    private final DocumentRepositoryPort documentRepository;
    private final ObjectMapper objectMapper;

    public DocumentListTool(DocumentRepositoryPort documentRepository, ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolDefinition definition() {
        Map<String, Object> props = JsonSchemaBuilder.toProperties(
            JsonSchemaBuilder.integerProp("limit", "Maximum number of documents to return (default 50)", false),
            JsonSchemaBuilder.integerProp("offset", "Number of documents to skip (default 0)", false)
        );
        return ToolDefinition.atomic(
            "document_list",
            "List all documents in the knowledge base with optional pagination.",
            JsonSchemaBuilder.objectSchema(List.of(), props),
            "rag"
        );
    }

    @Override
    public ToolExecutor executor() {
        return this::execute;
    }

    public ToolResult execute(ToolInvocation invocation) {
        int limit = invocation.getArg("limit", 50);
        int offset = invocation.getArg("offset", 0);

        try {
            List<Document> allDocs = documentRepository.findAll();

            int total = allDocs.size();
            List<Document> page = allDocs.stream()
                .skip(offset)
                .limit(limit)
                .toList();

            List<Map<String, Object>> docs = page.stream()
                .map(this::toMap)
                .toList();

            Map<String, Object> structured = Map.of(
                "total", total,
                "offset", offset,
                "limit", limit,
                "documents", docs
            );

            String content = objectMapper.writeValueAsString(structured);
            return ToolResult.success(content, structured);
        } catch (Exception e) {
            return ToolResult.error("Failed to list documents: " + e.getMessage());
        }
    }

    private Map<String, Object> toMap(Document doc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", doc.getId().value().toString());
        m.put("title", doc.getTitle());
        m.put("fileName", doc.getFileName());
        m.put("fileSize", doc.getFileSize());
        m.put("status", doc.getStatus().name());
        m.put("createdAt", doc.getCreatedAt().toString());
        m.put("updatedAt", doc.getUpdatedAt().toString());
        return m;
    }
}
