package com.ai.common.domain.repository;

/**
 * Web search capabilities for tool calling.
 */
public interface WebSearchTool {

    String searchWeb(String query);
}
