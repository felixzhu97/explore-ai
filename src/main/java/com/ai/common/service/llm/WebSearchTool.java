package com.ai.common.service.llm;

/** Web search capabilities for tool calling. */
public interface WebSearchTool {
  /** Searches the web. */
  String searchWeb(String query);
}
