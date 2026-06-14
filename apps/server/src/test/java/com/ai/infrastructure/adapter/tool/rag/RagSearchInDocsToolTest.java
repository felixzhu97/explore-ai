package com.ai.infrastructure.adapter.tool.rag;

import com.ai.application.port.EmbeddingPort;
import com.ai.application.port.VectorSearchPort;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagSearchInDocsTool")
class RagSearchInDocsToolTest {

    @Mock
    private EmbeddingPort embeddingPort;

    @Mock
    private VectorSearchPort vectorSearchPort;

    private RagSearchInDocsTool tool;

    @BeforeEach
    void setUp() {
        tool = new RagSearchInDocsTool(embeddingPort, vectorSearchPort);
    }

    @Nested
    @DisplayName("definition")
    class Definition {

        @Test
        @DisplayName("should have correct tool name")
        void shouldHaveCorrectToolName() {
            assertThat(tool.definition().name()).isEqualTo("rag_search_in_docs");
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
        @DisplayName("should return error for empty query")
        void shouldReturnErrorForEmptyQuery() {
            ToolResult result = tool.execute(new ToolInvocation("rag_search_in_docs",
                Map.of("query", "", "documentIds", List.of("abc"))));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("empty");
        }

        @Test
        @DisplayName("should return error for empty documentIds")
        void shouldReturnErrorForEmptyDocumentIds() {
            ToolResult result = tool.execute(new ToolInvocation("rag_search_in_docs",
                Map.of("query", "test", "documentIds", List.of())));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("empty");
        }

        @Test
        @DisplayName("should return error for invalid UUID format")
        void shouldReturnErrorForInvalidUuidFormat() {
            ToolResult result = tool.execute(new ToolInvocation("rag_search_in_docs",
                Map.of("query", "test", "documentIds", List.of("invalid-uuid"))));

            assertThat(result.isError()).isTrue();
        }
    }
}
