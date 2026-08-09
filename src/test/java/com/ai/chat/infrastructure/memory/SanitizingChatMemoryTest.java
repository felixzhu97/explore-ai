package com.ai.chat.infrastructure.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SanitizingChatMemory")
class SanitizingChatMemoryTest {

    @Test
    void shouldDropAssistantMessageWhenOnlyDsmlMarkup() {
        FakeChatMemory fake = new FakeChatMemory();
        SanitizingChatMemory memory = new SanitizingChatMemory(fake);

        memory.add("c1", List.of(
                new UserMessage("chart please"),
                new AssistantMessage("<｜DSML｜tool_calls><｜DSML｜invoke name=\"searchWeb\"></｜DSML｜tool_calls>")));

        assertThat(fake.get("c1")).hasSize(1);
        assertThat(fake.get("c1").get(0)).isInstanceOf(UserMessage.class);
        assertThat(memory.get("c1")).hasSize(1);
    }

    @Test
    void shouldKeepSanitizedAssistantTextWhenMarkupMixedWithProse() {
        FakeChatMemory fake = new FakeChatMemory();
        SanitizingChatMemory memory = new SanitizingChatMemory(fake);

        memory.add("c1", List.of(new AssistantMessage(
                "前言\n<｜DSML｜tool_calls>x</｜DSML｜tool_calls>\n后记")));

        assertThat(memory.get("c1")).hasSize(1);
        assertThat(memory.get("c1").get(0).getText()).contains("前言");
        assertThat(memory.get("c1").get(0).getText()).contains("后记");
        assertThat(memory.get("c1").get(0).getText()).doesNotContain("DSML");
    }

    private static final class FakeChatMemory implements ChatMemory {
        private final Map<String, List<org.springframework.ai.chat.messages.Message>> store =
                new ConcurrentHashMap<>();

        @Override
        public void add(String conversationId, List<org.springframework.ai.chat.messages.Message> messages) {
            store.computeIfAbsent(conversationId, id -> new ArrayList<>()).addAll(messages);
        }

        @Override
        public List<org.springframework.ai.chat.messages.Message> get(String conversationId) {
            return List.copyOf(store.getOrDefault(conversationId, List.of()));
        }

        @Override
        public void clear(String conversationId) {
            store.remove(conversationId);
        }
    }
}
