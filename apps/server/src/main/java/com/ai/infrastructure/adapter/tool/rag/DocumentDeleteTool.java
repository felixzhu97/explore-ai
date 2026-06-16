package com.ai.infrastructure.adapter.tool.rag;

import com.ai.application.usecase.DeleteDocumentUseCase;
import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.infrastructure.adapter.tool.ToolProvider;
import com.ai.infrastructure.adapter.tool.JsonSchemaBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tool for deleting a document.
 */
@Component
public class DocumentDeleteTool implements ToolProvider {

    private final DeleteDocumentUseCase deleteDocumentUseCase;
    private final ObjectMapper objectMapper;

    public DocumentDeleteTool(DeleteDocumentUseCase deleteDocumentUseCase, ObjectMapper objectMapper) {
        this.deleteDocumentUseCase = deleteDocumentUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolDefinition definition() {
        Map<String, Object> props = JsonSchemaBuilder.toProperties(
            JsonSchemaBuilder.stringProp("documentId", "The document UUID to delete", true)
        );
        return ToolDefinition.atomic(
            "document_delete",
            "Delete a document and all its chunks from the knowledge base.",
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
            deleteDocumentUseCase.execute(docId);

            Map<String, Object> result = Map.of(
                "deleted", true,
                "documentId", docIdStr
            );

            String content = objectMapper.writeValueAsString(result);
            return ToolResult.success(content, result);
        } catch (IllegalArgumentException e) {
            return ToolResult.error("Invalid document ID format: " + docIdStr);
        } catch (Exception e) {
            return ToolResult.error("Failed to delete document: " + e.getMessage());
        }
    }
}
