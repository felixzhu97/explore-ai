package com.ai.common.infrastructure.llm;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards the tool-calling loop after at least one tool result exists:
 * subsequent model rounds must answer (e.g. chart) without further tool calls.
 */
final class ToolCallLoopGuard {

    static final String AFTER_TOOLS_REMINDER = """
            Tool results are already in the conversation. Produce your final answer now.
            If a chart helps, emit the a2ui fence with chartData taken from the tool results.
            Do not call any tools again (including searchWeb).
            """;

    private ToolCallLoopGuard() {}

    static boolean hasToolResults(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        return messages.stream().anyMatch(ToolResponseMessage.class::isInstance);
    }

    static ChatOptions disableFurtherToolUse(ChatOptions options) {
        if (!(options instanceof ToolCallingChatOptions toolOptions)) {
            return options;
        }
        if (options instanceof OpenAiChatOptions openAi) {
            return openAi.mutate()
                    .toolCallbacks(List.of())
                    .toolChoice("none")
                    .build();
        }
        return toolOptions.mutate()
                .toolCallbacks(List.of())
                .build();
    }

    static List<Message> withFinalAnswerReminder(List<Message> history) {
        List<Message> next = new ArrayList<>(history);
        next.add(new SystemMessage(AFTER_TOOLS_REMINDER));
        return List.copyOf(next);
    }
}
