package com.ai.common.infrastructure.llm;

import com.ai.common.application.llm.TextChatOptions;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WeatherTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ChatClientFactory MCP merge")
class ChatClientFactoryMcpMergeTest {

    @Test
    @DisplayName("should_build_client_when_mcp_callbacks_absent")
    void should_build_client_when_mcp_callbacks_absent() {
        ChatClientFactory factory = factoryWithMcp(null);
        assertThatCode(() -> factory.createStateless(TextChatOptions.defaults()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should_build_client_when_mcp_callbacks_present")
    void should_build_client_when_mcp_callbacks_present() {
        ToolCallback mcp = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                        .name("fetch")
                        .description("fetch url")
                        .inputSchema("{}")
                        .build();
            }

            @Override
            public String call(String toolInput) {
                return "fetched";
            }
        };
        ChatClientFactory factory = factoryWithMcp(new ToolCallback[]{mcp});
        assertThatCode(() -> factory.createStateless(TextChatOptions.of("openai", null, true)))
                .doesNotThrowAnyException();
    }

    private static ChatClientFactory factoryWithMcp(ToolCallback[] mcp) {
        ChatModelResolver resolver = mock(ChatModelResolver.class);
        ChatModel chatModel = mock(ChatModel.class);
        when(resolver.resolve(any())).thenReturn(new ResolvedChatModel(
                chatModel,
                OpenAiChatOptions.builder().model("test"),
                "openai"));

        @SuppressWarnings("unchecked")
        ObjectProvider<ToolCallback[]> mcpProvider = mock(ObjectProvider.class);
        when(mcpProvider.getIfAvailable()).thenReturn(mcp);

        return new ChatClientFactory(
                resolver,
                mock(org.springframework.ai.chat.memory.ChatMemory.class),
                new PromptTemplates(),
                new StubWeatherTool(),
                new StubDocumentSearchTool(),
                new StubWebSearchTool(),
                mcpProvider,
                false);
    }

    static class StubWeatherTool implements WeatherTool {
        @Tool(description = "weather")
        public String getWeather(String city) {
            return city;
        }
    }

    static class StubDocumentSearchTool implements DocumentSearchTool {
        @Override
        @Tool(description = "search docs")
        public String searchDocuments(String query, List<String> docIds) {
            return query;
        }

        @Override
        @Tool(description = "list docs")
        public String listDocuments() {
            return "[]";
        }
    }

    static class StubWebSearchTool implements WebSearchTool {
        @Override
        @Tool(description = "search web")
        public String searchWeb(String query) {
            return query;
        }
    }
}
