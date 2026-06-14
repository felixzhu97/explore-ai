package com.ai.infrastructure.adapter.tool.rag;

import com.ai.application.port.DocumentRepositoryPort;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.domain.model.Document;
import com.ai.domain.model.DocumentStatus;
import com.ai.domain.vo.DocumentId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentListTool")
class DocumentListToolTest {

    @Mock
    private DocumentRepositoryPort documentRepository;

    private DocumentListTool tool;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tool = new DocumentListTool(documentRepository, objectMapper);
    }

    @Nested
    @DisplayName("definition")
    class Definition {

        @Test
        @DisplayName("should have correct tool name")
        void shouldHaveCorrectToolName() {
            assertThat(tool.definition().name()).isEqualTo("document_list");
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
        @DisplayName("should return document list with pagination")
        void shouldReturnDocumentListWithPagination() {
            Document doc = new Document(
                DocumentId.of(UUID.randomUUID()),
                "Test Doc",
                "test.txt",
                1024L
            );
            when(documentRepository.findAll()).thenReturn(List.of(doc));

            ToolResult result = tool.execute(new ToolInvocation("document_list", Map.of()));

            assertThat(result.isError()).isFalse();
            assertThat(result.structured()).containsKey("documents");
            assertThat(result.structured()).containsKey("total");
        }

        @Test
        @DisplayName("should support pagination parameters")
        void shouldSupportPaginationParameters() {
            when(documentRepository.findAll()).thenReturn(List.of());

            tool.execute(new ToolInvocation("document_list", Map.of("limit", 10, "offset", 5)));

            verify(documentRepository).findAll();
        }
    }
}
