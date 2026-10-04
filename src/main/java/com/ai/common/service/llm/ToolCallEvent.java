package com.ai.common.service.llm;

/**
 * Stream event announcing that the model invoked a tool.
 *
 * @param type always {@code tool_call}
 * @param input raw tool arguments as sent by the model, empty when absent
 */
public record ToolCallEvent(String type, String name, String input) {

  public static ToolCallEvent of(String name, String input) {
    return new ToolCallEvent("tool_call", name, input == null ? "" : input);
  }
}
