package com.ai.common.domain.tool;

import java.util.List;

/** Document search capabilities for tool calling and MCP. */
public interface DocumentSearchTool {
  String searchDocuments(String query, List<String> documentIds);

  String listDocuments();
}
