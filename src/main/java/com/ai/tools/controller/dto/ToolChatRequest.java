package com.ai.tools.controller.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Question sent to the chat that may call local tools. */
public record ToolChatRequest(@NotBlank String question, List<String> documentIds) {}
