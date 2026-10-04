package com.ai.tools.controller.dto;

import java.util.List;

/** Question sent to the chat that may call local tools. */
public record ToolChatRequest(String question, List<String> documentIds) {}
