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
 * Tool for getting a single document by ID.
 */
@Component
public class DocumentGetTool implements ToolProvider {

    private final DocumentRepositoryPort documentRepository;
    private final ObjectMapper objectMapper;

    public DocumentGetTool(DocumentRepositoryPort documentRepository, ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolDefinition definition() {
        Map<String, Object> props = JsonSchemaBuilder.toProperties(
            JsonSchemaBuilder.stringProp("documentId", "The document UUID", true)
        );
        return ToolDefinition.atomic(
            "document_get",
            "Get a single document's metadata by its ID.",
            JsonSchemaBuilder.objectSchema(List.of("documentId"), props),
            "rag"
        );
    }

    @Override
    public ToolExecutor executor() {
        return this::execute;
    }

    public ToolResult execute(ToolInvocation invocation) {
        String docIdStr = invocation.getArg("documentId", "");

        if (docIdStr == null || docIdStr.isBlank()) {
            return ToolResult.error("documentId is required");
        }

        try {
            UUID docId = UUID.fromString(docIdStr.trim());
            Optional<Document> docOpt = documentRepository.findById(docId);

            if (docOpt.isEmpty()) {
                return ToolResult.error("Document not found: " + docIdStr);
            }

            Document doc = docOpt.get();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", doc.getId().value().toString());
            m.put("title", doc.getTitle());
            m.put("fileName", doc.getFileName());
            m.put("fileSize", doc.getFileSize());
            m.put("status", doc.getStatus().name());
            m.put("createdAt", doc.getCreatedAt().toString());
            m.put("updatedAt", doc.getUpdatedAt().toString());

            String content = objectMapper.writeValueAsString(m);
            return ToolResult.success(content, m);
        } catch (IllegalArgumentException e) {
            return ToolResult.error("Invalid document ID format: " + docIdStr);
        } catch (Exception e) {
            return ToolResult.error("Failed to get document: " + e.getMessage());
        }
    }
}
