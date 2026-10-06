package com.ai.common.domain.tool;

/** Web search capabilities for tool calling. */
public interface WebSearchTool {
  /** Searches the web. */
  String searchWeb(String query);
}
