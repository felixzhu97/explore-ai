package com.ai.common.infrastructure.llm;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityChecker;
import org.springframework.ai.model.tool.ToolExecutionResult;

import java.util.List;

/**
 * Allows bridge tools (getCurrentDateTime) before a terminal retrieval tool,
 * then disables further tool use and reminds the model to answer (search → chart).
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Tool Calling</a>
 * @see <a href="https://api-docs.deepseek.com/guides/function_calling">DeepSeek Function Calling</a>
 */
public final class AnswerAfterToolsAdvisor extends ToolCallingAdvisor {

    private AnswerAfterToolsAdvisor(
            ToolCallingManager toolCallingManager,
            ToolExecutionEligibilityChecker toolExecutionEligibilityChecker,
            int advisorOrder,
            boolean conversationHistoryEnabled) {
        super(toolCallingManager, toolExecutionEligibilityChecker, advisorOrder, conversationHistoryEnabled);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getName() {
        return "Answer After Tools Advisor";
    }

    @Override
    protected ChatClientRequest doBeforeCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return maybeDisableTools(chatClientRequest);
    }

    @Override
    protected ChatClientRequest doBeforeStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return maybeDisableTools(chatClientRequest);
    }

    @Override
    protected List<Message> doGetNextInstructionsForToolCall(
            ChatClientRequest chatClientRequest,
            ChatClientResponse chatClientResponse,
            ToolExecutionResult toolExecutionResult) {
        return withStageReminder(
                super.doGetNextInstructionsForToolCall(chatClientRequest, chatClientResponse, toolExecutionResult));
    }

    @Override
    protected List<Message> doGetNextInstructionsForToolCallStream(
            ChatClientRequest chatClientRequest,
            ChatClientResponse chatClientResponse,
            ToolExecutionResult toolExecutionResult) {
        return withStageReminder(
                super.doGetNextInstructionsForToolCallStream(
                        chatClientRequest, chatClientResponse, toolExecutionResult));
    }

    private static List<Message> withStageReminder(List<Message> history) {
        if (ToolCallLoopGuard.shouldForceFinalAnswer(history)) {
            return ToolCallLoopGuard.withFinalAnswerReminder(history);
        }
        if (ToolCallLoopGuard.hasOnlyBridgeToolResults(history)) {
            return ToolCallLoopGuard.withContinuationReminder(history);
        }
        return history;
    }

    private static ChatClientRequest maybeDisableTools(ChatClientRequest request) {
        List<Message> instructions = request.prompt().getInstructions();
        if (!ToolCallLoopGuard.shouldForceFinalAnswer(instructions)) {
            return request;
        }
        ChatOptions disabled = ToolCallLoopGuard.disableFurtherToolUse(request.prompt().getOptions());
        return ChatClientRequest.builder()
                .prompt(new Prompt(instructions, disabled))
                .context(request.context())
                .build();
    }

    public static final class Builder extends ToolCallingAdvisor.Builder<Builder> {

        @Override
        protected Builder newCopy() {
            return new Builder();
        }

        @Override
        public AnswerAfterToolsAdvisor build() {
            return new AnswerAfterToolsAdvisor(
                    getToolCallingManager(),
                    getToolExecutionEligibilityChecker(),
                    getAdvisorOrder(),
                    isConversationHistoryEnabled());
        }
    }
}
