package com.ai.common.infrastructure.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ToolCallLoopGuard")
class ToolCallLoopGuardTest {

    @Nested
    @DisplayName("hasToolResults")
    class HasToolResults {

        @Test
        void should_returnFalse_when_noMessages() {
            assertThat(ToolCallLoopGuard.hasToolResults(List.of())).isFalse();
            assertThat(ToolCallLoopGuard.hasToolResults(null)).isFalse();
        }

        @Test
        void should_returnFalse_when_onlyUserAndAssistant() {
            assertThat(ToolCallLoopGuard.hasToolResults(List.of(
                    new SystemMessage("sys"),
                    new UserMessage("hi"),
                    new AssistantMessage("hello")))).isFalse();
        }

        @Test
        void should_returnTrue_when_toolResponsePresent() {
            ToolResponseMessage toolResponse = ToolResponseMessage.builder()
                    .responses(List.of(new ToolResponseMessage.ToolResponse(
                            "call-1", "searchWeb", "results")))
                    .build();

            assertThat(ToolCallLoopGuard.hasToolResults(List.of(
                    new UserMessage("chart please"),
                    toolResponse))).isTrue();
        }
    }

    @Nested
    @DisplayName("disableFurtherToolUse")
    class DisableFurtherToolUse {

        @Test
        void should_setToolChoiceNoneAndClearCallbacks_when_openAiOptions() {
            OpenAiChatOptions original = OpenAiChatOptions.builder()
                    .model("deepseek-v4-flash")
                    .toolChoice("auto")
                    .build();

            var disabled = ToolCallLoopGuard.disableFurtherToolUse(original);

            assertThat(disabled).isInstanceOf(OpenAiChatOptions.class);
            OpenAiChatOptions openAi = (OpenAiChatOptions) disabled;
            assertThat(openAi.getToolChoice()).isEqualTo("none");
            assertThat(openAi.getToolCallbacks()).isNullOrEmpty();
            assertThat(openAi.getModel()).isEqualTo("deepseek-v4-flash");
        }

        @Test
        void should_clearToolCallbacks_when_ollamaOptions() {
            OllamaChatOptions original = OllamaChatOptions.builder()
                    .model("llama3.2")
                    .build();

            var disabled = ToolCallLoopGuard.disableFurtherToolUse(original);

            assertThat(disabled).isInstanceOf(OllamaChatOptions.class);
            OllamaChatOptions ollama = (OllamaChatOptions) disabled;
            assertThat(ollama.getToolCallbacks()).isNullOrEmpty();
            assertThat(ollama.getModel()).isEqualTo("llama3.2");
        }

        @Test
        void should_clearToolCallbacks_when_anthropicOptions() {
            AnthropicChatOptions original = AnthropicChatOptions.builder()
                    .model("claude-sonnet-4-5")
                    .build();

            var disabled = ToolCallLoopGuard.disableFurtherToolUse(original);

            assertThat(disabled).isInstanceOf(AnthropicChatOptions.class);
            AnthropicChatOptions anthropic = (AnthropicChatOptions) disabled;
            assertThat(anthropic.getToolCallbacks()).isNullOrEmpty();
            assertThat(anthropic.getModel()).isEqualTo("claude-sonnet-4-5");
        }

        @Test
        void should_returnSameInstance_when_unknownChatOptions() {
            ChatOptions original = ChatOptions.builder().model("custom-model").build();

            var disabled = ToolCallLoopGuard.disableFurtherToolUse(original);

            assertThat(disabled).isSameAs(original);
        }
    }

    @Nested
    @DisplayName("withFinalAnswerReminder")
    class WithFinalAnswerReminder {

        @Test
        void should_appendReminderSystemMessage() {
            List<Message> history = List.of(new UserMessage("q"), new AssistantMessage("a"));
            List<Message> next = ToolCallLoopGuard.withFinalAnswerReminder(history);

            assertThat(next).hasSize(3);
            assertThat(next.get(2)).isInstanceOf(SystemMessage.class);
            assertThat(next.get(2).getText()).contains("Do not call any tools again");
            assertThat(next.get(2).getText()).contains("a2ui");
        }
    }
}
