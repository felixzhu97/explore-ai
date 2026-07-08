package com.ai.mcp.web;

import com.ai.mcp.application.usecase.McpFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp/client")
@Tag(name = "MCP Client", description = "Connect to external MCP servers and use their tools")
public class McpClientController {

    private static final Logger log = LoggerFactory.getLogger(McpClientController.class);

    private final McpFacade mcpFacade;

    public McpClientController(McpFacade mcpFacade) {
        this.mcpFacade = mcpFacade;
    }

    @GetMapping("/status")
    @Operation(summary = "Get MCP Client status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "READY",
                "registeredTools", mcpFacade.getTotalToolCount(),
                "connectedServers", mcpFacade.getConnectedServers().keySet().stream().toList()));
    }

    @GetMapping("/tools")
    @Operation(summary = "List all registered MCP tools")
    public ResponseEntity<List<Map<String, String>>> listTools() {
        List<Map<String, String>> tools = mcpFacade.getToolDefinitions().stream()
                .map(def -> Map.of("name", def.name(), "description", def.description()))
                .toList();
        return ResponseEntity.ok(tools);
    }

    @GetMapping("/servers")
    @Operation(summary = "List connected MCP servers")
    public ResponseEntity<List<Map<String, Object>>> listServers() {
        List<Map<String, Object>> servers = mcpFacade.getConnectedServers().values().stream()
                .map(info -> Map.<String, Object>of(
                        "name", info.name(),
                        "toolCount", info.toolCount(),
                        "status", info.status().name()))
                .toList();
        return ResponseEntity.ok(servers);
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI using MCP tools")
    public ResponseEntity<Map<String, String>> chat(@RequestBody McpChatRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "提问内容不能为空"));
        }
        try {
            String response = mcpFacade.chatWithTools(request.question());
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            log.error("Error in MCP chat", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "处理请求时发生错误，请稍后重试。"));
        }
    }

    public record McpChatRequest(String question, List<String> docIds) {}
}
