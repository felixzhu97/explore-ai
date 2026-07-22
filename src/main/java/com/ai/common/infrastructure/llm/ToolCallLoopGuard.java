package com.ai.common.infrastructure.llm;

import com.ai.common.infrastructure.prompt.ClasspathPromptLoader;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards the tool-calling loop after at least one tool result exists:
 * subsequent model rounds must answer (e.g. chart) without further tool calls.
 */
final class ToolCallLoopGuard {

    static final String AFTER_TOOLS_REMINDER = ClasspathPromptLoader.load("guards/after-tools.st");

    private ToolCallLoopGuard() {}

    static boolean hasToolResults(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        return messages.stream().anyMatch(ToolResponseMessage.class::isInstance);
    }

    static ChatOptions disableFurtherToolUse(ChatOptions options) {
        if (!(options instanceof ToolCallingChatOptions)) {
            return options;
        }
        if (options instanceof OpenAiChatOptions openAi) {
            return openAi.mutate()
                    .toolCallbacks(List.of())
                    .toolChoice("none")
                    .build();
        }
        if (options instanceof OllamaChatOptions ollama) {
            return ollama.mutate()
                    .toolCallbacks(List.of())
                    .build();
        }
        if (options instanceof AnthropicChatOptions anthropic) {
            return anthropic.mutate()
                    .toolCallbacks(List.of())
                    .build();
        }
        return options;
    }

    static List<Message> withFinalAnswerReminder(List<Message> history) {
        List<Message> next = new ArrayList<>(history);
        next.add(new SystemMessage(AFTER_TOOLS_REMINDER));
        return List.copyOf(next);
    }
}
