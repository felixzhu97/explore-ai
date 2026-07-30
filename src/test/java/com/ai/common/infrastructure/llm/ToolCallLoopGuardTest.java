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
    @DisplayName("shouldForceFinalAnswer")
    class ShouldForceFinalAnswer {

        @Test
        void should_returnFalse_when_noToolResults() {
            assertThat(ToolCallLoopGuard.shouldForceFinalAnswer(List.of(
                    new UserMessage("hi"),
                    new AssistantMessage("hello")))).isFalse();
        }

        @Test
        void should_returnFalse_when_onlyGetCurrentDateTime() {
            assertThat(ToolCallLoopGuard.shouldForceFinalAnswer(List.of(
                    new UserMessage("today?"),
                    toolResponse("call-1", "getCurrentDateTime", "2026-07-26")))).isFalse();
        }

        @Test
        void should_returnTrue_when_searchWebPresent() {
            assertThat(ToolCallLoopGuard.shouldForceFinalAnswer(List.of(
                    new UserMessage("chart please"),
                    toolResponse("call-1", "searchWeb", "results")))).isTrue();
        }

        @Test
        void should_returnTrue_when_datetimeThenSearchWeb() {
            assertThat(ToolCallLoopGuard.shouldForceFinalAnswer(List.of(
                    toolResponse("call-1", "getCurrentDateTime", "now"),
                    toolResponse("call-2", "searchWeb", "hits")))).isTrue();
        }

        @Test
        void should_returnTrue_when_twoToolResponseRounds() {
            assertThat(ToolCallLoopGuard.shouldForceFinalAnswer(List.of(
                    toolResponse("call-1", "getCurrentDateTime", "now"),
                    toolResponse("call-2", "getCurrentDateTime", "now2")))).isTrue();
        }
    }

    @Nested
    @DisplayName("hasOnlyBridgeToolResults")
    class HasOnlyBridgeToolResults {

        @Test
        void should_returnTrue_when_onlyDateTimeTool() {
            assertThat(ToolCallLoopGuard.hasOnlyBridgeToolResults(List.of(
                    toolResponse("call-1", "getCurrentDateTime", "now")))).isTrue();
        }

        @Test
        void should_returnFalse_when_searchWebPresent() {
            assertThat(ToolCallLoopGuard.hasOnlyBridgeToolResults(List.of(
                    toolResponse("call-1", "getCurrentDateTime", "now"),
                    toolResponse("call-2", "searchWeb", "hits")))).isFalse();
        }
    }

    @Nested
    @DisplayName("hasToolResults")
    class HasToolResults {

        @Test
        void should_returnFalse_when_noMessages() {
            assertThat(ToolCallLoopGuard.hasToolResults(List.of())).isFalse();
            assertThat(ToolCallLoopGuard.hasToolResults(null)).isFalse();
        }

        @Test
        void should_returnTrue_when_toolResponsePresent() {
            assertThat(ToolCallLoopGuard.hasToolResults(List.of(
                    new UserMessage("chart please"),
                    toolResponse("call-1", "searchWeb", "results")))).isTrue();
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
            assertThat(((OllamaChatOptions) disabled).getToolCallbacks()).isNullOrEmpty();
        }

        @Test
        void should_clearToolCallbacks_when_anthropicOptions() {
            AnthropicChatOptions original = AnthropicChatOptions.builder()
                    .model("claude-sonnet-4-5")
                    .build();

            var disabled = ToolCallLoopGuard.disableFurtherToolUse(original);

            assertThat(disabled).isInstanceOf(AnthropicChatOptions.class);
            assertThat(((AnthropicChatOptions) disabled).getToolCallbacks()).isNullOrEmpty();
        }

        @Test
        void should_returnSameInstance_when_unknownChatOptions() {
            ChatOptions original = ChatOptions.builder().model("custom-model").build();
            assertThat(ToolCallLoopGuard.disableFurtherToolUse(original)).isSameAs(original);
        }
    }

    @Nested
    @DisplayName("reminders")
    class Reminders {

        @Test
        void should_appendFinalReminder_banning_furtherToolsIncludingFetch() {
            List<Message> next = ToolCallLoopGuard.withFinalAnswerReminder(
                    List.of(new UserMessage("q"), new AssistantMessage("a")));

            assertThat(next.get(2)).isInstanceOf(SystemMessage.class);
            assertThat(next.get(2).getText()).contains("Do not call any tools again");
            assertThat(next.get(2).getText()).contains("including searchWeb and fetch");
            assertThat(next.get(2).getText()).contains("a2ui");
            assertThat(next.get(2).getText()).contains("Do not output DSML");
        }

        @Test
        void should_appendContinuationReminder_allowing_searchWeb() {
            List<Message> next = ToolCallLoopGuard.withContinuationReminder(
                    List.of(toolResponse("call-1", "getCurrentDateTime", "now")));

            assertThat(next.get(1).getText()).contains("searchWeb exactly once");
            assertThat(next.get(1).getText()).contains("a2ui");
            assertThat(next.get(1).getText()).contains("Do not call fetch");
            assertThat(next.get(1).getText()).doesNotContain("Do not call any tools again");
        }
    }

    @Nested
    @DisplayName("withStageReminder")
    class WithStageReminder {

        @Test
        void should_appendFinalReminder_when_terminalToolPresent() {
            List<Message> next = ToolCallLoopGuard.withStageReminder(List.of(
                    toolResponse("call-1", "searchWeb", "hits")));

            assertThat(next).hasSize(2);
            assertThat(next.get(1).getText()).contains("Do not call any tools again");
        }

        @Test
        void should_appendBridgeReminder_when_onlyDateTimePresent() {
            List<Message> next = ToolCallLoopGuard.withStageReminder(List.of(
                    toolResponse("call-1", "getCurrentDateTime", "now")));

            assertThat(next).hasSize(2);
            assertThat(next.get(1).getText()).contains("searchWeb exactly once");
        }

        @Test
        void should_leaveHistoryUnchanged_when_noToolResults() {
            List<Message> history = List.of(new UserMessage("hi"));
            assertThat(ToolCallLoopGuard.withStageReminder(history)).isSameAs(history);
        }
    }

    @Nested
    @DisplayName("maybeDisableToolsRequest")
    class MaybeDisableToolsRequest {

        @Test
        void should_disableTools_when_terminalToolPresent() {
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model("deepseek-v4-flash")
                    .toolChoice("auto")
                    .build();
            var request = org.springframework.ai.chat.client.ChatClientRequest.builder()
                    .prompt(new org.springframework.ai.chat.prompt.Prompt(
                            List.of(toolResponse("call-1", "searchWeb", "hits")), options))
                    .build();

            var adjusted = ToolCallLoopGuard.maybeDisableToolsRequest(request);

            OpenAiChatOptions disabled = (OpenAiChatOptions) adjusted.prompt().getOptions();
            assertThat(disabled.getToolChoice()).isEqualTo("none");
        }
    }

    private static ToolResponseMessage toolResponse(String id, String name, String data) {
        return ToolResponseMessage.builder()
                .responses(List.of(new ToolResponseMessage.ToolResponse(id, name, data)))
                .build();
    }
}
