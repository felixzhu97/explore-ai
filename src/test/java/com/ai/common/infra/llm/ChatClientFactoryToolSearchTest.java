package com.ai.common.infra.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ai.common.infra.prompt.PromptTemplates;
import com.ai.common.service.llm.DocumentSearchTool;
import com.ai.common.service.llm.WebSearchTool;
import com.ai.tools.domain.model.WeatherSimulator;
import com.ai.tools.infra.tools.DateTimeTools;
import com.ai.tools.infra.tools.WeatherTools;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;

@DisplayName("ChatClientFactoryToolSearch")
class ChatClientFactoryToolSearchTest {

  @Test
  void shouldUseToolCallLoopGuardAdvisorWhenToolSearchDisabled() {
    ChatClientFactory factory = newFactory(false);

    Advisor advisor = factory.createToolCallingAdvisor();

    assertThat(factory.isToolSearchEnabled()).isFalse();
    assertThat(advisor).isInstanceOf(ToolCallLoopGuardAdvisor.class);
    assertThat(advisor).isNotInstanceOf(ToolSearchToolCallingAdvisor.class);
  }

  @Test
  void shouldUseToolSearchToolCallingAdvisorWhenToolSearchEnabled() {
    ChatClientFactory factory = newFactory(true);

    Advisor advisor = factory.createToolCallingAdvisor();

    assertThat(factory.isToolSearchEnabled()).isTrue();
    assertThat(advisor).isInstanceOf(ToolSearchToolCallingAdvisor.class);
  }

  @SuppressWarnings("unchecked")
  private static ChatClientFactory newFactory(boolean toolSearchEnabled) {
    return new ChatClientFactory(
        mock(ChatModelResolver.class),
        mock(ChatMemory.class),
        new PromptTemplates(),
        new WeatherTools(new WeatherSimulator()),
        new StubDocumentSearchTool(),
        new StubWebSearchTool(),
        new DateTimeTools(Clock.systemUTC()),
        mock(ObjectProvider.class),
        toolSearchEnabled);
  }

  static class StubDocumentSearchTool implements DocumentSearchTool {
    @Override
    @Tool(description = "search docs")
    public String searchDocuments(String query, List<String> documentIds) {
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
