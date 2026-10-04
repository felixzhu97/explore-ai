package com.ai.common.domain.tool;

/** Web search capabilities for tool calling. */
public interface WebSearchTool {
  String searchWeb(String query);
}
