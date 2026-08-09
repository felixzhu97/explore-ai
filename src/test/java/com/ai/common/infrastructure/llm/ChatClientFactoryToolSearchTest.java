package com.ai.common.infrastructure.llm;

import com.ai.common.domain.repository.DateTimeTool;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.common.domain.repository.WeatherTool;
import com.ai.common.domain.repository.WebSearchTool;
import com.ai.common.infrastructure.prompt.PromptTemplates;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
                new StubWeatherTool(),
                new StubDocumentSearchTool(),
                new StubWebSearchTool(),
                new StubDateTimeTool(),
                mock(ObjectProvider.class),
                false,
                toolSearchEnabled);
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
        @Tool(description = "datetime")
        public String getCurrentDateTime() {
            return "now";
        }
    }
}
