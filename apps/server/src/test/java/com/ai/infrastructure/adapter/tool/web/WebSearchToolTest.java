package com.ai.infrastructure.adapter.tool.web;

import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.infrastructure.adapter.web.WebSearchPort;
import com.ai.infrastructure.adapter.web.WebSearchPort.WebSearchException;
import com.ai.infrastructure.adapter.web.WebSearchResult;
import com.ai.infrastructure.config.WebSearchProperties;
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
import static org.mockito.Mockito.when;

/**
 * Unit tests for WebSearchTool.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSearchTool")
class WebSearchToolTest {

    @Mock
    private WebSearchPort webSearchPort;

    private WebSearchProperties webSearchProperties;
    private WebSearchTool webSearchTool;

    @BeforeEach
    void setUp() {
        webSearchProperties = new WebSearchProperties();
        webSearchProperties.setDefaultMaxResults(5);
        webSearchProperties.setDefaultRegion("us-en");
        webSearchTool = new WebSearchTool(webSearchPort, webSearchProperties);
    }

    @Nested
    @DisplayName("definition")
    class DefinitionTests {

        @Test
        @DisplayName("should return correct tool definition")
        void shouldReturnCorrectToolDefinition() {
            ToolDefinition definition = webSearchTool.definition();

            assertThat(definition.name()).isEqualTo("web_search");
            assertThat(definition.category()).isEqualTo("web");
            assertThat(definition.composite()).isFalse();
            assertThat(definition.description()).isNotEmpty();
        }

        @Test
        @DisplayName("should have query as required parameter")
        void shouldHaveQueryAsRequiredParameter() {
            ToolDefinition definition = webSearchTool.definition();
            Map<String, Object> schema = definition.inputSchema();

            assertThat(schema).containsEntry("type", "object");
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            assertThat(properties).containsKey("query");
            @SuppressWarnings("unchecked")
            List<String> required = (List<String>) schema.get("required");
            assertThat(required).contains("query");
        }

        @Test
        @DisplayName("should have optional maxResults and region parameters")
        void shouldHaveOptionalParameters() {
            ToolDefinition definition = webSearchTool.definition();
            Map<String, Object> schema = definition.inputSchema();

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            assertThat(properties).containsKey("maxResults");
            assertThat(properties).containsKey("region");
        }
    }

    @Nested
    @DisplayName("execute")
    class ExecuteTests {

        @Test
        @DisplayName("should return search results when successful")
        void shouldReturnSearchResultsWhenSuccessful() throws WebSearchException {
            // Arrange
            List<WebSearchResult> results = List.of(
                new WebSearchResult("Spring AI Documentation", "https://docs.spring.io/spring-ai", "Learn about Spring AI", 0),
                new WebSearchResult("Spring AI GitHub", "https://github.com/spring-projects/spring-ai", "Spring AI on GitHub", 1)
            );
            when(webSearchPort.search(eq("Spring AI"), eq(5), eq("us-en"))).thenReturn(results);

            ToolInvocation invocation = new ToolInvocation("web_search", Map.of("query", "Spring AI"));

            // Act
            ToolResult result = webSearchTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("Web Search Results");
            assertThat(result.content()).contains("Spring AI Documentation");
            assertThat(result.content()).contains("https://docs.spring.io/spring-ai");
            assertThat(result.content()).contains("Learn about Spring AI");
        }

        @Test
        @DisplayName("should use custom maxResults when provided")
        void shouldUseCustomMaxResultsWhenProvided() throws WebSearchException {
            // Arrange
            when(webSearchPort.search(eq("test"), eq(10), anyString())).thenReturn(List.of());

            ToolInvocation invocation = new ToolInvocation("web_search", Map.of(
                "query", "test",
                "maxResults", 10
            ));

            // Act
            webSearchTool.execute(invocation);

            // Assert
            // Verify the adapter was called with maxResults=10
        }

        @Test
        @DisplayName("should clamp maxResults to valid range")
        void shouldClampMaxResultsToValidRange() throws WebSearchException {
            // Arrange
            when(webSearchPort.search(eq("test"), eq(20), anyString())).thenReturn(List.of());

            ToolInvocation invocation = new ToolInvocation("web_search", Map.of(
                "query", "test",
                "maxResults", 100  // Should be clamped to 20
            ));

            // Act
            webSearchTool.execute(invocation);

            // Assert - the call should use max=20
        }

        @Test
        @DisplayName("should use custom region when provided")
        void shouldUseCustomRegionWhenProvided() throws WebSearchException {
            // Arrange
            when(webSearchPort.search(eq("test"), anyInt(), eq("cn-zh"))).thenReturn(List.of());

            ToolInvocation invocation = new ToolInvocation("web_search", Map.of(
                "query", "test",
                "region", "cn-zh"
            ));

            // Act
            webSearchTool.execute(invocation);

            // Assert
        }

        @Test
        @DisplayName("should return empty message when no results")
        void shouldReturnEmptyMessageWhenNoResults() throws WebSearchException {
            // Arrange
            when(webSearchPort.search(anyString(), anyInt(), anyString())).thenReturn(List.of());

            ToolInvocation invocation = new ToolInvocation("web_search", Map.of("query", "xyz123"));

            // Act
            ToolResult result = webSearchTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("No results found");
        }

        @Test
        @DisplayName("should return error when query is empty")
        void shouldReturnErrorWhenQueryIsEmpty() {
            ToolInvocation invocation = new ToolInvocation("web_search", Map.of("query", ""));

            // Act
            ToolResult result = webSearchTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("Query is required");
        }

        @Test
        @DisplayName("should return error when rate limited")
        void shouldReturnErrorWhenRateLimited() throws WebSearchException {
            // Arrange
            when(webSearchPort.search(anyString(), anyInt(), anyString()))
                .thenThrow(new WebSearchException("Rate limit", 429));

            ToolInvocation invocation = new ToolInvocation("web_search", Map.of("query", "test"));

            // Act
            ToolResult result = webSearchTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("rate limit");
        }

        @Test
        @DisplayName("should return friendly error on HTTP failure")
        void shouldReturnFriendlyErrorOnHttpFailure() throws WebSearchException {
            // Arrange
            when(webSearchPort.search(anyString(), anyInt(), anyString()))
                .thenThrow(new WebSearchException("HTTP 500", 500));

            ToolInvocation invocation = new ToolInvocation("web_search", Map.of("query", "test"));

            // Act
            ToolResult result = webSearchTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("Web search failed");
        }

        @Test
        @DisplayName("should return structured data with results")
        void shouldReturnStructuredDataWithResults() throws WebSearchException {
            // Arrange
            List<WebSearchResult> results = List.of(
                new WebSearchResult("Title", "https://example.com", "Snippet", 0)
            );
            when(webSearchPort.search(anyString(), anyInt(), anyString())).thenReturn(results);

            ToolInvocation invocation = new ToolInvocation("web_search", Map.of("query", "test"));

            // Act
            ToolResult result = webSearchTool.execute(invocation);

            // Assert
            assertThat(result.structured()).isNotNull();
            assertThat(result.structured()).containsKey("results");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> structuredResults = (List<Map<String, Object>>) result.structured().get("results");
            assertThat(structuredResults).hasSize(1);
            assertThat(structuredResults.get(0)).containsEntry("title", "Title");
            assertThat(structuredResults.get(0)).containsEntry("url", "https://example.com");
        }
    }
}
