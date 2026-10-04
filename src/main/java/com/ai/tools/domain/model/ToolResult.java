package com.ai.tools.domain.model;

/** Outcome of a tool invocation: a success flag plus the content or failure message. */
public class ToolResult {

  private final boolean success;
  private final String content;

  private ToolResult(boolean success, String content) {
    this.success = success;
    this.content = content;
  }

  public static ToolResult success(String content) {
    return new ToolResult(true, content);
  }

  public static ToolResult failure(String message) {
    return new ToolResult(false, message);
  }

  public boolean isSuccess() {
    return success;
  }

  public String content() {
    return content;
  }
}
