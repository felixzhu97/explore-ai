package com.ai.mcp.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Question sent to the chat that may call MCP tools. */
public record McpChatRequest(@NotBlank String question, List<String> documentIds) {}
