package com.ai.tools.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Outcome of a tool invocation: a success flag plus the content or failure message. */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolResult {

  private final boolean success;
  private final String content;

  /** Creates a successful result. */
  public static ToolResult success(String content) {
    return new ToolResult(true, content);
  }

  /** Creates a failed result. */
  public static ToolResult failure(String message) {
    return new ToolResult(false, message);
  }
}
