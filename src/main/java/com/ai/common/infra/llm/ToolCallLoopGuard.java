package com.ai.common.infra.llm;

import com.ai.common.infra.prompt.ClasspathPromptLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * Guards the tool-calling loop so bridge tools (e.g. getCurrentDateTime) can run before a terminal
 * retrieval tool (e.g. searchWeb), then forces a final answer.
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/api/tools.html">Spring AI Tool
 *     Calling</a>
 * @see <a href="https://api-docs.deepseek.com/guides/function_calling">DeepSeek Function
 *     Calling</a>
 */
final class ToolCallLoopGuard {

  static final String AFTER_TOOLS_REMINDER = ClasspathPromptLoader.load("guards/after-tools.st");
  static final String AFTER_BRIDGE_REMINDER =
      ClasspathPromptLoader.load("guards/after-bridge-tool.st");

  /** Safety cap: datetime → searchWeb → done. */
  private static final int MAX_TOOL_ROUNDS = 2;

  private static final Set<String> BRIDGE_TOOL_NAMES = Set.of("getcurrentdatetime");

  private static final Set<String> TERMINAL_TOOL_NAMES =
      Set.of(
          "searchweb",
          "fetch",
          "getweather",
          "getforecast",
          "searchdocuments",
          "search_documents",
          "listdocuments",
          "list_documents");

  private ToolCallLoopGuard() {}

  /** Tells whether the messages include tool results. */
  static boolean hasToolResults(List<Message> messages) {
    return filterToolResponseMessages(messages).size() > 0;
  }

  /**
   * Force final answer after a terminal retrieval tool, or after two tool-response rounds (datetime
   * → search → done).
   */
  static boolean shouldForceFinalAnswer(List<Message> messages) {
    List<ToolResponseMessage> toolMessages = filterToolResponseMessages(messages);
    if (toolMessages.isEmpty()) {
      return false;
    }
    if (toolMessages.size() >= MAX_TOOL_ROUNDS) {
      return true;
    }
    return streamToolNames(toolMessages).anyMatch(ToolCallLoopGuard::isTerminalTool);
  }

  /** Tells whether every tool result came from a bridge tool. */
  static boolean hasOnlyBridgeToolResults(List<Message> messages) {
    List<ToolResponseMessage> toolMessages = filterToolResponseMessages(messages);
    if (toolMessages.isEmpty()) {
      return false;
    }
    return streamToolNames(toolMessages).allMatch(ToolCallLoopGuard::isBridgeTool);
  }

  /** Returns options that stop further tool calls. */
  static ChatOptions disableFurtherToolUse(ChatOptions options) {
    if (!(options instanceof ToolCallingChatOptions)) {
      return options;
    }
    if (options instanceof OpenAiChatOptions openAi) {
      return openAi.mutate().toolCallbacks(List.of()).toolChoice("none").build();
    }
    if (options instanceof OllamaChatOptions ollama) {
      return ollama.mutate().toolCallbacks(List.of()).build();
    }
    if (options instanceof AnthropicChatOptions anthropic) {
      return anthropic.mutate().toolCallbacks(List.of()).build();
    }
    return options;
  }

  /** Adds a reminder to write the final answer. */
  static List<Message> withFinalAnswerReminder(List<Message> history) {
    List<Message> next = new ArrayList<>(history);
    next.add(new SystemMessage(AFTER_TOOLS_REMINDER));
    return List.copyOf(next);
  }

  /** Adds a reminder to continue after a bridge tool. */
  static List<Message> withContinuationReminder(List<Message> history) {
    List<Message> next = new ArrayList<>(history);
    next.add(new SystemMessage(AFTER_BRIDGE_REMINDER));
    return List.copyOf(next);
  }

  /** Adds the reminder that fits the current tool stage. */
  static List<Message> withStageReminder(List<Message> history) {
    if (shouldForceFinalAnswer(history)) {
      return withFinalAnswerReminder(history);
    }
    if (hasOnlyBridgeToolResults(history)) {
      return withContinuationReminder(history);
    }
    return history;
  }

  /** Turns off tools when the model must give its final answer. */
  static org.springframework.ai.chat.client.ChatClientRequest disableToolsIfNeeded(
      org.springframework.ai.chat.client.ChatClientRequest request) {
    List<Message> instructions = request.prompt().getInstructions();
    if (!shouldForceFinalAnswer(instructions)) {
      return request;
    }
    ChatOptions disabled = disableFurtherToolUse(request.prompt().getOptions());
    return org.springframework.ai.chat.client.ChatClientRequest.builder()
        .prompt(new org.springframework.ai.chat.prompt.Prompt(instructions, disabled))
        .context(request.context())
        .build();
  }

  private static List<ToolResponseMessage> filterToolResponseMessages(List<Message> messages) {
    if (messages == null || messages.isEmpty()) {
      return List.of();
    }
    return messages.stream()
        .filter(ToolResponseMessage.class::isInstance)
        .map(ToolResponseMessage.class::cast)
        .toList();
  }

  private static Stream<String> streamToolNames(List<ToolResponseMessage> toolMessages) {
    return toolMessages.stream()
        .flatMap(message -> message.getResponses().stream())
        .map(ToolResponseMessage.ToolResponse::name);
  }

  private static boolean isTerminalTool(String name) {
    return name != null && TERMINAL_TOOL_NAMES.contains(name.toLowerCase(Locale.ROOT));
  }

  private static boolean isBridgeTool(String name) {
    return name != null && BRIDGE_TOOL_NAMES.contains(name.toLowerCase(Locale.ROOT));
  }
}
