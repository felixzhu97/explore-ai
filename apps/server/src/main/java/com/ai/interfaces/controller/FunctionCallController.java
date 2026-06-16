package com.ai.interfaces.controller;

import com.ai.application.port.ToolRegistryPort;
import com.ai.application.tool.ToolInvocation;
import com.ai.interfaces.dto.functioncall.FunctionCallInvokeRequest;
import com.ai.interfaces.dto.functioncall.ToolDefinitionDto;
import com.ai.interfaces.dto.functioncall.ToolResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for OpenAI-style Function Call endpoints.
 *
 * <p>Available endpoints:</p>
 * <ul>
 *   <li>{@code GET /api/function-call/tools} — list all registered tools</li>
 *   <li>{@code POST /api/function-call/invoke} — invoke a tool by name</li>
 * </ul>
 *
 * @since 0.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/function-call")
@RequiredArgsConstructor
@Tag(name = "FunctionCall", description = "OpenAI-style Function Call endpoints")
public class FunctionCallController {

    private final ToolRegistryPort toolRegistry;

    /**
     * Lists all registered tools.
     *
     * @return list of tool definitions
     */
    @GetMapping("/tools")
    @Operation(summary = "List tools", description = "Returns all registered tool definitions")
    public ResponseEntity<List<ToolDefinitionDto>> listTools() {
        log.debug("Listing all registered tools");
        List<ToolDefinitionDto> tools = toolRegistry.listTools().stream()
            .map(ToolDefinitionDto::from)
            .toList();
        return ResponseEntity.ok(tools);
    }

    /**
     * Invokes a registered tool by name with the given arguments.
     *
     * @param request invoke request containing toolName and arguments
     * @return tool execution result
     */
    @PostMapping("/invoke")
    @Operation(summary = "Invoke a tool", description = "Invokes a registered tool by name")
    public ResponseEntity<ToolResultDto> invoke(@Valid @RequestBody FunctionCallInvokeRequest request) {
        log.info("Tool invoke request: toolName={}", request.toolName());
        ToolInvocation invocation = new ToolInvocation(
            request.toolName(),
            request.arguments(),
            request.sessionId()
        );
        ToolResultDto result = ToolResultDto.from(toolRegistry.invoke(invocation));
        return ResponseEntity.ok(result);
    }
}
