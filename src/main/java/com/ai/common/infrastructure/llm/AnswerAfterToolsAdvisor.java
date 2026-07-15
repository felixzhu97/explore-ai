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
 * After tool results land in the conversation, disable further tool use and
 * remind the model to answer immediately (search once → chart).
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/2.0/api/tools.html">Tool Calling</a>
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
        return ToolCallLoopGuard.withFinalAnswerReminder(
                super.doGetNextInstructionsForToolCall(chatClientRequest, chatClientResponse, toolExecutionResult));
    }

    @Override
    protected List<Message> doGetNextInstructionsForToolCallStream(
            ChatClientRequest chatClientRequest,
            ChatClientResponse chatClientResponse,
            ToolExecutionResult toolExecutionResult) {
        return ToolCallLoopGuard.withFinalAnswerReminder(
                super.doGetNextInstructionsForToolCallStream(
                        chatClientRequest, chatClientResponse, toolExecutionResult));
    }

    private static ChatClientRequest maybeDisableTools(ChatClientRequest request) {
        List<Message> instructions = request.prompt().getInstructions();
        if (!ToolCallLoopGuard.hasToolResults(instructions)) {
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
