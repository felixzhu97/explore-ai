package com.ai.common.infra.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolExecutionResult;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatOptions;

@DisplayName("ToolCallLoopGuardAdvisor parity")
class ToolCallLoopGuardAdvisorTest {

  private static final ToolCallLoopGuardAdvisor ADVISOR =
      ToolCallLoopGuardAdvisor.builder().build();

  @Nested
  @DisplayName("should disable tools before model call")
  class DisableToolsBeforeCall {

    @Test
    void shouldDisableToolsWhenSearchWebAlreadyRan() {
      ChatClientRequest request =
          requestWithHistory(
              List.of(
                  new UserMessage("chart please"), toolResponse("call-1", "searchWeb", "results")));

      ChatClientRequest adjusted = invokeBeforeCall(request);

      OpenAiChatOptions options = (OpenAiChatOptions) adjusted.prompt().getOptions();
      assertThat(options.getToolChoice()).isEqualTo("none");
      assertThat(options.getToolCallbacks()).isNullOrEmpty();
    }

    @Test
    void shouldKeepToolsEnabledWhenOnlyGetCurrentDateTime() {
      ChatClientRequest request =
          requestWithHistory(
              List.of(
                  new UserMessage("today?"),
                  toolResponse("call-1", "getCurrentDateTime", "2026-07-26")));

      ChatClientRequest adjusted = invokeBeforeCall(request);

      OpenAiChatOptions options = (OpenAiChatOptions) adjusted.prompt().getOptions();
      assertThat(options.getToolChoice()).isEqualTo("auto");
    }
  }

  @Nested
  @DisplayName("should enrich tool history via LoopGuardToolCallingManager")
  class StageRemindersViaManager {

    @Test
    void shouldAppendFinalReminderWhenTerminalToolExecuted() {
      ToolExecutionResult raw =
          DefaultToolExecutionResult.builder()
              .conversationHistory(
                  List.of(new UserMessage("chart"), toolResponse("call-1", "searchWeb", "hits")))
              .build();
      ToolCallingManager stub = stubManagerReturning(raw);

      LoopGuardToolCallingManager manager = new LoopGuardToolCallingManager(stub);
      ToolExecutionResult enriched =
          manager.executeToolCalls(mock(Prompt.class), mock(ChatResponse.class));

      assertThat(enriched.conversationHistory()).hasSize(3);
      assertThat(enriched.conversationHistory().get(2)).isInstanceOf(SystemMessage.class);
      assertThat(enriched.conversationHistory().get(2).getText())
          .contains("Do not call any tools again");
    }

    @Test
    void shouldAppendBridgeReminderWhenOnlyDateTimeExecuted() {
      ToolExecutionResult raw =
          DefaultToolExecutionResult.builder()
              .conversationHistory(List.of(toolResponse("call-1", "getCurrentDateTime", "now")))
              .build();
      ToolCallingManager stub = stubManagerReturning(raw);

      LoopGuardToolCallingManager manager = new LoopGuardToolCallingManager(stub);
      ToolExecutionResult enriched =
          manager.executeToolCalls(mock(Prompt.class), mock(ChatResponse.class));

      assertThat(enriched.conversationHistory()).hasSize(2);
      assertThat(enriched.conversationHistory().get(1).getText())
          .contains("searchWeb exactly once");
    }
  }

  @Nested
  @DisplayName("should preserve AnswerAfterTools advisor chain behavior")
  class AdvisorChainParity {

    @Test
    void shouldPassDisabledRequestToChainWhenTerminalToolPresent() {
      AtomicReference<ChatClientRequest> captured = new AtomicReference<>();
      CallAdvisorChain chain = mock(CallAdvisorChain.class);
      when(chain.copy(any())).thenReturn(chain);
      when(chain.nextCall(any()))
          .thenAnswer(
              invocation -> {
                captured.set(invocation.getArgument(0));
                return ChatClientResponse.builder()
                    .chatResponse(
                        ChatResponse.builder()
                            .generations(List.of(new Generation(new AssistantMessage("done"))))
                            .build())
                    .build();
              });

      ChatClientRequest request =
          requestWithHistory(
              List.of(new UserMessage("news"), toolResponse("call-1", "searchWeb", "snippets")));

      ADVISOR.adviseCall(request, chain);

      assertThat(captured.get()).isNotNull();
      OpenAiChatOptions options = (OpenAiChatOptions) captured.get().prompt().getOptions();
      assertThat(options.getToolChoice()).isEqualTo("none");
    }
  }

  private static ChatClientRequest invokeBeforeCall(ChatClientRequest request) {
    AtomicReference<ChatClientRequest> captured = new AtomicReference<>();
    CallAdvisorChain chain = mock(CallAdvisorChain.class);
    when(chain.copy(any())).thenReturn(chain);
    when(chain.nextCall(any()))
        .thenAnswer(
            invocation -> {
              captured.set(invocation.getArgument(0));
              return ChatClientResponse.builder()
                  .chatResponse(
                      ChatResponse.builder()
                          .generations(List.of(new Generation(new AssistantMessage("ok"))))
                          .build())
                  .build();
            });

    ADVISOR.adviseCall(request, chain);
    return captured.get();
  }

  private static ChatClientRequest requestWithHistory(List<Message> history) {
    OpenAiChatOptions options =
        OpenAiChatOptions.builder().model("deepseek-v4-flash").toolChoice("auto").build();
    return ChatClientRequest.builder()
        .prompt(new Prompt(new ArrayList<>(history), options))
        .build();
  }

  private static ToolCallingManager stubManagerReturning(ToolExecutionResult result) {
    ToolCallingManager delegate = mock(ToolCallingManager.class);
    when(delegate.executeToolCalls(any(), any())).thenReturn(result);
    return delegate;
  }

  private static ToolResponseMessage toolResponse(String id, String name, String data) {
    return ToolResponseMessage.builder()
        .responses(List.of(new ToolResponseMessage.ToolResponse(id, name, data)))
        .build();
  }
}
