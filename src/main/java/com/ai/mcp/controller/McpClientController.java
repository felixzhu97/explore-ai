package com.ai.mcp.controller;

import com.ai.mcp.controller.dto.McpChatRequest;
import com.ai.mcp.service.McpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
  public ResponseEntity<Map<String, Object>> getStatus() {
    return ResponseEntity.ok(
        Map.of(
            "status", "READY",
            "registeredTools", mcpService.getTotalToolCount(),
            "connectedServers", mcpService.getConnectedServers().keySet().stream().toList()));
  }

  @GetMapping("/tools")
  @Operation(summary = "List all registered MCP tools")
  public ResponseEntity<List<Map<String, String>>> listTools() {
    List<Map<String, String>> tools =
        mcpService.getToolDefinitions().stream()
            .map(def -> Map.of("name", def.name(), "description", def.description()))
            .toList();
    return ResponseEntity.ok(tools);
  }

  @GetMapping("/servers")
  @Operation(summary = "List connected MCP servers")
  public ResponseEntity<List<Map<String, Object>>> listServers() {
    List<Map<String, Object>> servers =
        mcpService.getConnectedServers().values().stream()
            .map(
                info ->
                    Map.<String, Object>of(
                        "name", info.name(),
                        "toolCount", info.toolCount(),
                        "status", info.status().name()))
            .toList();
    return ResponseEntity.ok(servers);
  }

  @PostMapping("/chat")
  @Operation(summary = "Chat with AI using MCP tools")
  public Map<String, String> chat(@Valid @RequestBody McpChatRequest request) {
    return Map.of("response", mcpService.chatWithTools(request.question()));
  }
}
