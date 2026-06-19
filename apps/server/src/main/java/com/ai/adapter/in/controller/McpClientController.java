package com.ai.adapter.in.controller;

import com.ai.adapter.out.mcp.AiMcpClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for MCP Client operations.
 * Provides endpoints to interact with external MCP servers.
 */
@RestController
@RequestMapping("/api/mcp/client")
@Tag(name = "MCP Client", description = "Connect to external MCP servers and use their tools")
public class McpClientController {

    private final AiMcpClientService mcpClientService;
    private final ChatClient chatClient;

    public McpClientController(AiMcpClientService mcpClientService, ChatClient.Builder chatClientBuilder) {
        this.mcpClientService = mcpClientService;
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/status")
    @Operation(summary = "Get MCP Client status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "READY",
                "registeredTools", mcpClientService.getTotalToolCount(),
                "connectedServers", mcpClientService.getConnectedServers().keySet().stream().toList()
        ));
    }

    @GetMapping("/tools")
    @Operation(summary = "List all registered MCP tools")
    public ResponseEntity<List<Map<String, String>>> listTools() {
        List<Map<String, String>> tools = mcpClientService.getToolDefinitions().stream()
                .map(def -> Map.<String, String>of(
                        "name", def.name(),
                        "description", def.description() != null ? def.description() : ""
                ))
                .toList();
        return ResponseEntity.ok(tools);
    }

    @GetMapping("/servers")
    @Operation(summary = "List connected MCP servers")
    public ResponseEntity<List<Map<String, Object>>> listServers() {
        List<Map<String, Object>> servers = mcpClientService.getConnectedServers().values().stream()
                .map(info -> Map.<String, Object>of(
                        "name", info.name(),
                        "toolCount", info.toolCount(),
                        "status", info.status()
                ))
                .toList();
        return ResponseEntity.ok(servers);
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI using MCP tools")
    public ResponseEntity<Map<String, String>> chat(@RequestBody McpChatRequest request) {
        try {
            var tools = mcpClientService.getRegisteredTools().toArray(new org.springframework.ai.tool.ToolCallback[0]);

            String response = chatClient.prompt()
                    .user(request.question())
                    .tools(tools)
                    .call()
                    .content();

            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record McpChatRequest(String question, List<String> docIds) {
    }
}
