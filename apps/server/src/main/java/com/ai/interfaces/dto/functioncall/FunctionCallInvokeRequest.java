package com.ai.interfaces.dto.functioncall;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Function call invoke request DTO.
 *
 * @since 0.2.0
 */
public record FunctionCallInvokeRequest(
    @NotBlank(message = "toolName is required")
    String toolName,

    Map<String, Object> arguments,

    String sessionId
) {
    public FunctionCallInvokeRequest {
        if (arguments == null) {
            arguments = Map.of();
        }
    }
}
