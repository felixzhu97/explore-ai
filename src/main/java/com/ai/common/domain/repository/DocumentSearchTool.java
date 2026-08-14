package com.ai.common.domain.repository;

import java.util.List;

/** Document search capabilities for tool calling and MCP. */
public interface DocumentSearchTool {
  /** Documentation. */
  String searchDocuments(String query, List<String> docIds);

  /** Documentation. */
  String listDocuments();
}
