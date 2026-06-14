package com.ai.infrastructure.adapter.tool.rag;

import com.ai.application.usecase.DeleteDocumentUseCase;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentDeleteTool")
class DocumentDeleteToolTest {

    @Mock
    private DeleteDocumentUseCase deleteDocumentUseCase;

    private DocumentDeleteTool tool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tool = new DocumentDeleteTool(deleteDocumentUseCase, objectMapper);
    }

    @Nested
    @DisplayName("definition")
    class Definition {

        @Test
        @DisplayName("should have correct tool name")
        void shouldHaveCorrectToolName() {
            assertThat(tool.definition().name()).isEqualTo("document_delete");
        }

        @Test
        @DisplayName("should have rag category")
        void shouldHaveRagCategory() {
            assertThat(tool.definition().category()).isEqualTo("rag");
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should return error for missing documentId")
        void shouldReturnErrorForMissingDocumentId() {
            ToolResult result = tool.execute(new ToolInvocation("document_delete", Map.of("documentId", "")));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("required");
        }

        @Test
        @DisplayName("should return error for invalid UUID")
        void shouldReturnErrorForInvalidUuid() {
            ToolResult result = tool.execute(new ToolInvocation("document_delete",
                Map.of("documentId", "not-a-uuid")));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("Invalid");
        }

        @Test
        @DisplayName("should delete document and return success")
        void shouldDeleteDocumentAndReturnSuccess() {
            UUID docId = UUID.randomUUID();
            doNothing().when(deleteDocumentUseCase).execute(docId);

            ToolResult result = tool.execute(new ToolInvocation("document_delete",
                Map.of("documentId", docId.toString())));

            assertThat(result.isError()).isFalse();
            assertThat(result.structured()).containsEntry("deleted", true);
            verify(deleteDocumentUseCase).execute(docId);
        }

        @Test
        @DisplayName("should handle delete failure")
        void shouldHandleDeleteFailure() {
            UUID docId = UUID.randomUUID();
            doThrow(new RuntimeException("Delete failed")).when(deleteDocumentUseCase).execute(docId);

            ToolResult result = tool.execute(new ToolInvocation("document_delete",
                Map.of("documentId", docId.toString())));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("Failed to delete");
        }
    }
}
