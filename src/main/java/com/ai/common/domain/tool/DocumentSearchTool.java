package com.ai.common.domain.tool;

import java.util.List;

/** Document search capabilities for tool calling and MCP. */
public interface DocumentSearchTool {
  /** Searches the uploaded documents. */
  String searchDocuments(String query, List<String> documentIds);

  /** Lists the uploaded documents. */
  String listDocuments();
}
