package com.ai.infrastructure.adapter.tool.rag;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.domain.model.Document;
import com.ai.domain.vo.DocumentId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentGetTool")
class DocumentGetToolTest {

    @Mock
    private DocumentRepositoryPort documentRepository;

    private DocumentGetTool tool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tool = new DocumentGetTool(documentRepository, objectMapper);
    }

    @Nested
    @DisplayName("definition")
    class Definition {

        @Test
        @DisplayName("should have correct tool name")
        void shouldHaveCorrectToolName() {
            assertThat(tool.definition().name()).isEqualTo("document_get");
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
            ToolResult result = tool.execute(new ToolInvocation("document_get", Map.of("documentId", "")));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("required");
        }

        @Test
        @DisplayName("should return error for invalid UUID")
        void shouldReturnErrorForInvalidUuid() {
            ToolResult result = tool.execute(new ToolInvocation("document_get",
                Map.of("documentId", "not-a-uuid")));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("Invalid");
        }

        @Test
        @DisplayName("should return error for non-existent document")
        void shouldReturnErrorForNonExistentDocument() {
            UUID docId = UUID.randomUUID();
            when(documentRepository.findById(docId)).thenReturn(Optional.empty());

            ToolResult result = tool.execute(new ToolInvocation("document_get",
                Map.of("documentId", docId.toString())));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("not found");
        }

        @Test
        @DisplayName("should return document data")
        void shouldReturnDocumentData() {
            UUID docId = UUID.randomUUID();
            Document doc = new Document(
                DocumentId.of(docId),
                "Test Doc",
                "test.txt",
                1024L
            );
            when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

            ToolResult result = tool.execute(new ToolInvocation("document_get",
                Map.of("documentId", docId.toString())));

            assertThat(result.isError()).isFalse();
            assertThat(result.structured()).containsEntry("title", "Test Doc");
        }
    }
}
