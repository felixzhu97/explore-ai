package com.ai.common.service.llm;

/**
 * Stream event carrying the outcome of a tool invocation.
 *
 * @param type always {@code tool_result}
 * @param output tool result truncated for display, or the failure message
 */
public record ToolResultEvent(String type, String name, boolean ok, String output) {

  /** Creates a successful tool result event. */
  public static ToolResultEvent success(String name, String output) {
    return new ToolResultEvent("tool_result", name, true, output);
  }

  /** Creates a failed tool result event. */
  public static ToolResultEvent failure(String name, String output) {
    return new ToolResultEvent("tool_result", name, false, output);
  }
}
