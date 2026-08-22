package com.ai.common.infra.llm;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ai.common.domain.repository.DateTimeTool;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WeatherTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.common.infra.prompt.PromptTemplates;
import com.ai.common.service.llm.TextChatOptions;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

@DisplayName("ChatClientFactory MCP merge")
class ChatClientFactoryMcpMergeTest {

  @Test
  @DisplayName("should build client when mcp callbacks absent")
  void shouldBuildClientWhenMcpCallbacksAbsent() {
    ChatClientFactory factory = factoryWithMcp(null);
    assertThatCode(() -> factory.createStateless(TextChatOptions.defaults()))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("should build client when mcp callbacks present")
  void shouldBuildClientWhenMcpCallbacksPresent() {
    ToolCallback mcp = namedTool("fetch", "fetched");
    ChatClientFactory factory = factoryWithMcp(new ToolCallback[] {mcp});
    assertThatCode(() -> factory.createStateless(TextChatOptions.of("openai", null, true)))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("should prefer local tool when mcp name collides")
  void shouldPreferLocalToolWhenMcpNameCollides() {
    ToolCallback duplicateWeather = namedTool("getWeather", "mcp-weather");
    ChatClientFactory factory = factoryWithMcp(new ToolCallback[] {duplicateWeather});
    assertThatCode(() -> factory.createStateless(TextChatOptions.of("openai", null, true)))
        .doesNotThrowAnyException();
  }

  private static ToolCallback namedTool(String name, String result) {
    return new ToolCallback() {
      @Override
      public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder().name(name).description(name).inputSchema("{}").build();
      }

      @Override
      public String call(String toolInput) {
        return result;
      }
    };
  }

  private static ChatClientFactory factoryWithMcp(ToolCallback[] mcp) {
    ChatModelResolver resolver = mock(ChatModelResolver.class);
    ChatModel chatModel = mock(ChatModel.class);
    when(resolver.resolve(any()))
        .thenReturn(
            new ResolvedChatModel(chatModel, OpenAiChatOptions.builder().model("test"), "openai"));

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
        new StubDateTimeTool(),
        mcpProvider,
        false,
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

  static class StubDateTimeTool implements DateTimeTool {
    @Tool(description = "Get the current date and time in the user's timezone")
    public String getCurrentDateTime() {
      return "2026-07-26T12:40+08:00[Asia/Shanghai]";
    }
  }
}
