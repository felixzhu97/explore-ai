package com.ai.common.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

@DisplayName("ToolCallLoopGuard")
class ToolCallLoopGuardTest {

  @Nested
  @DisplayName("shouldForceFinalAnswer")
  class ShouldForceFinalAnswer {

    @Test
    void shouldReturnFalseWhenNoToolResults() {
      assertThat(
              ToolCallLoopGuard.shouldForceFinalAnswer(
                  List.of(new UserMessage("hi"), new AssistantMessage("hello"))))
          .isFalse();
    }

    @Test
    void shouldReturnFalseWhenOnlyGetCurrentDateTime() {
      assertThat(
              ToolCallLoopGuard.shouldForceFinalAnswer(
                  List.of(
                      new UserMessage("today?"),
                      toolResponse("call-1", "getCurrentDateTime", "2026-07-26"))))
          .isFalse();
    }

    @Test
    void shouldReturnTrueWhenSearchWebPresent() {
      assertThat(
              ToolCallLoopGuard.shouldForceFinalAnswer(
                  List.of(
                      new UserMessage("chart please"),
                      toolResponse("call-1", "searchWeb", "results"))))
          .isTrue();
    }

    @Test
    void shouldReturnTrueWhenDatetimeThenSearchWeb() {
      assertThat(
              ToolCallLoopGuard.shouldForceFinalAnswer(
                  List.of(
                      toolResponse("call-1", "getCurrentDateTime", "now"),
                      toolResponse("call-2", "searchWeb", "hits"))))
          .isTrue();
    }

    @Test
    void shouldReturnTrueWhenTwoToolResponseRounds() {
      assertThat(
              ToolCallLoopGuard.shouldForceFinalAnswer(
                  List.of(
                      toolResponse("call-1", "getCurrentDateTime", "now"),
                      toolResponse("call-2", "getCurrentDateTime", "now2"))))
          .isTrue();
    }
  }

  @Nested
  @DisplayName("hasOnlyBridgeToolResults")
  class HasOnlyBridgeToolResults {

    @Test
    void shouldReturnTrueWhenOnlyDateTimeTool() {
      assertThat(
              ToolCallLoopGuard.hasOnlyBridgeToolResults(
                  List.of(toolResponse("call-1", "getCurrentDateTime", "now"))))
          .isTrue();
    }

    @Test
    void shouldReturnFalseWhenSearchWebPresent() {
      assertThat(
              ToolCallLoopGuard.hasOnlyBridgeToolResults(
                  List.of(
                      toolResponse("call-1", "getCurrentDateTime", "now"),
                      toolResponse("call-2", "searchWeb", "hits"))))
          .isFalse();
    }
  }

  @Nested
  @DisplayName("hasToolResults")
  class HasToolResults {

    @Test
    void shouldReturnFalseWhenNoMessages() {
      assertThat(ToolCallLoopGuard.hasToolResults(List.of())).isFalse();
      assertThat(ToolCallLoopGuard.hasToolResults(null)).isFalse();
    }

    @Test
    void shouldReturnTrueWhenToolResponsePresent() {
      assertThat(
              ToolCallLoopGuard.hasToolResults(
                  List.of(
                      new UserMessage("chart please"),
                      toolResponse("call-1", "searchWeb", "results"))))
          .isTrue();
    }
  }

  @Nested
  @DisplayName("disableFurtherToolUse")
  class DisableFurtherToolUse {

    @Test
    void shouldSetToolChoiceNoneAndClearCallbacksWhenOpenAiOptions() {
      OpenAiChatOptions original =
          OpenAiChatOptions.builder().model("deepseek-v4-flash").toolChoice("auto").build();

      var disabled = ToolCallLoopGuard.disableFurtherToolUse(original);

      assertThat(disabled).isInstanceOf(OpenAiChatOptions.class);
      OpenAiChatOptions openAi = (OpenAiChatOptions) disabled;
      assertThat(openAi.getToolChoice()).isEqualTo("none");
      assertThat(openAi.getToolCallbacks()).isNullOrEmpty();
      assertThat(openAi.getModel()).isEqualTo("deepseek-v4-flash");
    }

    @Test
    void shouldClearToolCallbacksWhenOllamaOptions() {
      OllamaChatOptions original = OllamaChatOptions.builder().model("llama3.2").build();

      var disabled = ToolCallLoopGuard.disableFurtherToolUse(original);

      assertThat(disabled).isInstanceOf(OllamaChatOptions.class);
      assertThat(((OllamaChatOptions) disabled).getToolCallbacks()).isNullOrEmpty();
    }

    @Test
    void shouldClearToolCallbacksWhenAnthropicOptions() {
      AnthropicChatOptions original =
          AnthropicChatOptions.builder().model("claude-sonnet-4-5").build();

      var disabled = ToolCallLoopGuard.disableFurtherToolUse(original);

      assertThat(disabled).isInstanceOf(AnthropicChatOptions.class);
      assertThat(((AnthropicChatOptions) disabled).getToolCallbacks()).isNullOrEmpty();
    }

    @Test
    void shouldReturnSameInstanceWhenUnknownChatOptions() {
      ChatOptions original = ChatOptions.builder().model("custom-model").build();
      assertThat(ToolCallLoopGuard.disableFurtherToolUse(original)).isSameAs(original);
    }
  }

  @Nested
  @DisplayName("reminders")
  class Reminders {

    @Test
    void shouldAppendFinalReminderBanningFurtherToolsIncludingFetch() {
      List<Message> next =
          ToolCallLoopGuard.withFinalAnswerReminder(
              List.of(new UserMessage("q"), new AssistantMessage("a")));

      assertThat(next.get(2)).isInstanceOf(SystemMessage.class);
      assertThat(next.get(2).getText()).contains("Do not call any tools again");
      assertThat(next.get(2).getText()).contains("including searchWeb and fetch");
      assertThat(next.get(2).getText()).contains("a2ui");
      assertThat(next.get(2).getText()).contains("Do not output DSML");
    }

    @Test
    void shouldAppendContinuationReminderAllowingSearchWeb() {
      List<Message> next =
          ToolCallLoopGuard.withContinuationReminder(
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
    void shouldAppendFinalReminderWhenTerminalToolPresent() {
      List<Message> next =
          ToolCallLoopGuard.withStageReminder(List.of(toolResponse("call-1", "searchWeb", "hits")));

      assertThat(next).hasSize(2);
      assertThat(next.get(1).getText()).contains("Do not call any tools again");
    }

    @Test
    void shouldAppendBridgeReminderWhenOnlyDateTimePresent() {
      List<Message> next =
          ToolCallLoopGuard.withStageReminder(
              List.of(toolResponse("call-1", "getCurrentDateTime", "now")));

      assertThat(next).hasSize(2);
      assertThat(next.get(1).getText()).contains("searchWeb exactly once");
    }

    @Test
    void shouldLeaveHistoryUnchangedWhenNoToolResults() {
      List<Message> history = List.of(new UserMessage("hi"));
      assertThat(ToolCallLoopGuard.withStageReminder(history)).isSameAs(history);
    }
  }

  @Nested
  @DisplayName("maybeDisableToolsRequest")
  class MaybeDisableToolsRequest {

    @Test
    void shouldDisableToolsWhenTerminalToolPresent() {
      OpenAiChatOptions options =
          OpenAiChatOptions.builder().model("deepseek-v4-flash").toolChoice("auto").build();
      var request =
          org.springframework.ai.chat.client.ChatClientRequest.builder()
              .prompt(
                  new org.springframework.ai.chat.prompt.Prompt(
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
