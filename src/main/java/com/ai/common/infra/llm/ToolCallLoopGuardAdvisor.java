package com.ai.common.infra.llm;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;

/**
 * Official {@link ToolCallingAdvisor} extension that disables further tool use after terminal
 * retrieval tools, with stage reminders supplied by {@link LoopGuardToolCallingManager}.
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Tool Calling</a>
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/advisors.html">Advisors</a>
 */
final class ToolCallLoopGuardAdvisor extends ToolCallingAdvisor {

  private ToolCallLoopGuardAdvisor(
      ToolCallingManager toolCallingManager,
      ToolExecutionEligibilityChecker toolExecutionEligibilityChecker,
      int advisorOrder,
      boolean conversationHistoryEnabled) {
    super(
        toolCallingManager,
        toolExecutionEligibilityChecker,
        advisorOrder,
        conversationHistoryEnabled);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String getName() {
    return "Tool Call Loop Guard Advisor";
  }

  @Override
  protected ChatClientRequest doBeforeCall(
      ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
    return ToolCallLoopGuard.maybeDisableToolsRequest(chatClientRequest);
  }

  @Override
  protected ChatClientRequest doBeforeStream(
      ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
    return ToolCallLoopGuard.maybeDisableToolsRequest(chatClientRequest);
  }

  static final class Builder extends ToolCallingAdvisor.Builder<Builder> {

    @Override
    protected Builder newCopy() {
      return new Builder();
    }

    @Override
    public ToolCallLoopGuardAdvisor build() {
      return new ToolCallLoopGuardAdvisor(
          new LoopGuardToolCallingManager(getToolCallingManager()),
          getToolExecutionEligibilityChecker(),
          getAdvisorOrder(),
          isConversationHistoryEnabled());
    }
  }
}
