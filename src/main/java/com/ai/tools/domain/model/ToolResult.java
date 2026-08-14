package com.ai.tools.domain.model;

/** Documentation. */
public class ToolResult {

  private final boolean success;
  private final String content;

  private ToolResult(boolean success, String content) {
    this.success = success;
    this.content = content;
  }

  /** Documentation. */
  public static ToolResult success(String content) {
    return new ToolResult(true, content);
  }

  /** Documentation. */
  public static ToolResult failure(String message) {
    return new ToolResult(false, message);
  }

  public boolean isSuccess() {
    return success;
  }

  /** Documentation. */
  public String content() {
    return content;
  }
}
