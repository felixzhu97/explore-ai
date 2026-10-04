package com.ai.mcp.controller;

import com.ai.mcp.controller.dto.McpChatRequest;
import com.ai.mcp.service.McpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger log = LoggerFactory.getLogger(McpClientController.class);

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
  public ResponseEntity<Map<String, String>> chat(@RequestBody McpChatRequest request) {
    if (request.question() == null || request.question().isBlank()) {
      return ResponseEntity.badRequest().body(Map.of("error", "提问内容不能为空"));
    }
    try {
      String response = mcpService.chatWithTools(request.question());
      return ResponseEntity.ok(Map.of("response", response));
    } catch (Exception e) {
      log.error("Error in MCP chat", e);
      return ResponseEntity.internalServerError().body(Map.of("error", "处理请求时发生错误，请稍后重试。"));
    }
  }
}
