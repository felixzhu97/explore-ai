package com.ai.common.domain;

/**
 * Web search capabilities for tool calling.
 */
public interface WebSearchTool {

    String searchWeb(String query);
}
