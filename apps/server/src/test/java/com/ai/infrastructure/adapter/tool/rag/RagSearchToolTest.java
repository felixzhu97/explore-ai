package com.ai.infrastructure.adapter.tool.rag;

import com.ai.application.port.EmbeddingPort;
import com.ai.application.port.VectorSearchPort;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.domain.model.DocumentChunk;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagSearchTool")
class RagSearchToolTest {

    @Mock
    private EmbeddingPort embeddingPort;

    @Mock
    private VectorSearchPort vectorSearchPort;

    private RagSearchTool tool;

    @BeforeEach
    void setUp() {
        tool = new RagSearchTool(embeddingPort, vectorSearchPort);
    }

    @Nested
    @DisplayName("definition")
    class Definition {

        @Test
        @DisplayName("should have correct tool name")
        void shouldHaveCorrectToolName() {
            assertThat(tool.definition().name()).isEqualTo("rag_search");
        }

        @Test
        @DisplayName("should have rag category")
        void shouldHaveRagCategory() {
            assertThat(tool.definition().category()).isEqualTo("rag");
        }

        @Test
        @DisplayName("should not be composite")
        void shouldNotBeComposite() {
            assertThat(tool.definition().composite()).isFalse();
        }
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should return error for empty query")
        void shouldReturnErrorForEmptyQuery() {
            ToolResult result = tool.execute(new ToolInvocation("rag_search", Map.of("query", "")));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("empty");
        }

        @Test
        @DisplayName("should return search results with content and structured data")
        void shouldReturnSearchResultsWithContentAndStructuredData() {
            when(embeddingPort.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
            DocumentChunk chunk = new DocumentChunk(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                "This is test content about transformers",
                0,
                Map.of("documentId", java.util.UUID.randomUUID().toString(), "title", "Test Doc")
            ).withEmbedding(new float[]{0.1f, 0.2f});
            when(vectorSearchPort.search(any(float[].class), anyInt())).thenReturn(List.of(chunk));

            ToolResult result = tool.execute(new ToolInvocation("rag_search", Map.of("query", "transformer")));

            assertThat(result.isError()).isFalse();
            assertThat(result.content()).isNotEmpty();
            assertThat(result.structured()).isNotNull();
            assertThat(result.structured()).containsKey("results");
        }

        @Test
        @DisplayName("should use default topK when not provided")
        void shouldUseDefaultTopKWhenNotProvided() {
            when(embeddingPort.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
            when(vectorSearchPort.search(any(float[].class), eq(5))).thenReturn(List.of());

            tool.execute(new ToolInvocation("rag_search", Map.of("query", "test")));

            verify(vectorSearchPort).search(any(float[].class), eq(5));
        }

        @Test
        @DisplayName("should use provided topK")
        void shouldUseProvidedTopK() {
            when(embeddingPort.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
            when(vectorSearchPort.search(any(float[].class), eq(10))).thenReturn(List.of());

            tool.execute(new ToolInvocation("rag_search", Map.of("query", "test", "topK", 10)));

            verify(vectorSearchPort).search(any(float[].class), eq(10));
        }
    }
}
