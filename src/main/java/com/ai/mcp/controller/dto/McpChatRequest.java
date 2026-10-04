package com.ai.mcp.controller.dto;

import java.util.List;

/** Question sent to the chat that may call MCP tools. */
public record McpChatRequest(String question, List<String> documentIds) {}
