package com.ai.mcp.controller;

import com.ai.mcp.controller.dto.McpChatRequest;
import com.ai.mcp.controller.dto.McpChatResponse;
import com.ai.mcp.controller.dto.McpClientStatusResponse;
import com.ai.mcp.controller.dto.McpServerResponse;
import com.ai.mcp.controller.dto.McpToolResponse;
import com.ai.mcp.service.McpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mcp/client")
@Tag(name = "MCP Client", description = "Connect to external MCP servers and use their tools")
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-mcp",
    havingValue = "true",
    matchIfMissing = false)
public class McpClientController {

  private final McpService mcpService;

  public McpClientController(McpService mcpService) {
    this.mcpService = mcpService;
  }

  @GetMapping("/status")
  @Operation(summary = "Get MCP Client status")
  public ResponseEntity<McpClientStatusResponse> getStatus() {
    return ResponseEntity.ok(
        new McpClientStatusResponse(
            McpClientStatusResponse.McpClientStatus.READY,
            mcpService.getTotalToolCount(),
            mcpService.getConnectedServers().keySet().stream().toList()));
  }

  @GetMapping("/tools")
  @Operation(summary = "List all registered MCP tools")
  public ResponseEntity<List<McpToolResponse>> listTools() {
    List<McpToolResponse> tools =
        mcpService.getToolDefinitions().stream()
            .map(def -> new McpToolResponse(def.name(), def.description()))
            .toList();
    return ResponseEntity.ok(tools);
  }

  @GetMapping("/servers")
  @Operation(summary = "List connected MCP servers")
  public ResponseEntity<List<McpServerResponse>> listServers() {
    return ResponseEntity.ok(
        mcpService.getConnectedServers().values().stream().map(McpServerResponse::from).toList());
  }

  @PostMapping("/chat")
  @Operation(summary = "Chat with AI using MCP tools")
  public McpChatResponse chat(@Valid @RequestBody McpChatRequest request) {
    return new McpChatResponse(mcpService.chatWithTools(request.question()));
  }
}
