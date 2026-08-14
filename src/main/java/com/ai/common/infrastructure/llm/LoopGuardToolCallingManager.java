package com.ai.common.infrastructure.llm;

import java.util.List;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolExecutionResult;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Decorates {@link ToolCallingManager} with loop-guard stage reminders after tool execution.
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Tool Calling</a>
 */
final class LoopGuardToolCallingManager implements ToolCallingManager {

  private final ToolCallingManager delegate;

  LoopGuardToolCallingManager(ToolCallingManager delegate) {
    this.delegate = delegate;
  }

  @Override
  public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
    return delegate.resolveToolDefinitions(chatOptions);
  }

  @Override
  public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
    ToolExecutionResult result = delegate.executeToolCalls(prompt, chatResponse);
    List<org.springframework.ai.chat.messages.Message> history =
        ToolCallLoopGuard.withStageReminder(result.conversationHistory());
    return DefaultToolExecutionResult.builder()
        .conversationHistory(history)
        .returnDirect(result.returnDirect())
        .build();
  }
}
