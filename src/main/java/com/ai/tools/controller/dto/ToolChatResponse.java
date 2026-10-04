package com.ai.tools.controller.dto;

import java.util.List;

/** Chat answer with the tool calls made while answering. */
public record ToolChatResponse(String answer, List<String> toolCalls) {}
