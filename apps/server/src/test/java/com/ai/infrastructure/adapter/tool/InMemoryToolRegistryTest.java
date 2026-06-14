package com.ai.infrastructure.adapter.tool;

import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("InMemoryToolRegistry")
class InMemoryToolRegistryTest {

    private InMemoryToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryToolRegistry(List.of());
    }

    @Nested
    @DisplayName("listTools")
    class ListTools {

        @Test
        @DisplayName("should return empty list when no tools registered")
        void shouldReturnEmptyListWhenNoToolsRegistered() {
            assertThat(registry.listTools()).isEmpty();
        }

        @Test
        @DisplayName("should return all registered tools")
        void shouldReturnAllRegisteredTools() {
            ToolProvider mockProvider = mock(ToolProvider.class);
            when(mockProvider.definition()).thenReturn(
                ToolDefinition.atomic("test_tool", "A test tool",
                    Map.of("type", "object"), "test")
            );
            when(mockProvider.executor()).thenReturn(inv -> ToolResult.success("ok"));

            InMemoryToolRegistry reg = new InMemoryToolRegistry(List.of(mockProvider));

            List<ToolDefinition> tools = reg.listTools();
            assertThat(tools).hasSize(1);
            assertThat(tools.get(0).name()).isEqualTo("test_tool");
        }

        @Test
        @DisplayName("should skip providers with null definition")
        void shouldSkipProvidersWithNullDefinition() {
            ToolProvider mockProvider = mock(ToolProvider.class);
            when(mockProvider.definition()).thenReturn(null);

            InMemoryToolRegistry reg = new InMemoryToolRegistry(List.of(mockProvider));

            assertThat(reg.listTools()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByName")
    class FindByName {

        @Test
        @DisplayName("should find tool by name")
        void shouldFindToolByName() {
            ToolProvider mockProvider = mock(ToolProvider.class);
            when(mockProvider.definition()).thenReturn(
                ToolDefinition.atomic("rag_search", "Search RAG",
                    Map.of("type", "object"), "rag")
            );
            when(mockProvider.executor()).thenReturn(inv -> ToolResult.success("ok"));

            InMemoryToolRegistry reg = new InMemoryToolRegistry(List.of(mockProvider));

            var def = reg.findByName("rag_search");
            assertThat(def).isPresent();
            assertThat(def.get().name()).isEqualTo("rag_search");
        }

        @Test
        @DisplayName("should return empty for unknown tool")
        void shouldReturnEmptyForUnknownTool() {
            assertThat(registry.findByName("unknown")).isEmpty();
        }
    }

    @Nested
    @DisplayName("invoke")
    class Invoke {

        @Test
        @DisplayName("should invoke tool and return result")
        void shouldInvokeToolAndReturnResult() throws Exception {
            ToolProvider mockProvider = mock(ToolProvider.class);
            when(mockProvider.definition()).thenReturn(
                ToolDefinition.atomic("test_tool", "A test tool",
                    Map.of("type", "object"), "test")
            );
            ToolExecutor executor = mock(ToolExecutor.class);
            when(executor.execute(any())).thenReturn(ToolResult.success("test result"));
            when(mockProvider.executor()).thenReturn(executor);

            InMemoryToolRegistry reg = new InMemoryToolRegistry(List.of(mockProvider));

            ToolResult result = reg.invoke(new ToolInvocation("test_tool", Map.of()));

            assertThat(result.isError()).isFalse();
            assertThat(result.content()).isEqualTo("test result");
            verify(executor).execute(any());
        }

        @Test
        @DisplayName("should return error for unknown tool")
        void shouldReturnErrorForUnknownTool() {
            ToolResult result = registry.invoke(new ToolInvocation("unknown_tool", Map.of()));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("not found");
        }

        @Test
        @DisplayName("should return error when executor throws")
        void shouldReturnErrorWhenExecutorThrows() {
            ToolProvider mockProvider = mock(ToolProvider.class);
            when(mockProvider.definition()).thenReturn(
                ToolDefinition.atomic("failing_tool", "A failing tool",
                    Map.of("type", "object"), "test")
            );
            when(mockProvider.executor()).thenReturn(inv -> { throw new RuntimeException("test error"); });

            InMemoryToolRegistry reg = new InMemoryToolRegistry(List.of(mockProvider));

            ToolResult result = reg.invoke(new ToolInvocation("failing_tool", Map.of()));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("failed");
        }
    }
}
