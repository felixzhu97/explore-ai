package com.ai.mcp.controller;

import com.ai.common.controller.dto.HealthStatus;
import com.ai.mcp.controller.dto.McpCapabilitiesResponse;
import com.ai.mcp.controller.dto.McpHealthResponse;
import com.ai.mcp.controller.dto.McpInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST Controller for MCP Server management endpoints. */
@RestController
@RequestMapping("/api/mcp")
@Tag(name = "MCP Server", description = "MCP Server management")
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-mcp",
    havingValue = "true",
    matchIfMissing = false)
public class McpController {
  @GetMapping("/health")
  @Operation(summary = "MCP Server health check")
  public ResponseEntity<McpHealthResponse> getHealth() {
    return ResponseEntity.ok(
        new McpHealthResponse(HealthStatus.UP, "explore-ai-mcp-server", "1.0.0", "MCP 1.0"));
  }

  @GetMapping("/info")
  @Operation(summary = "Get MCP Server information")
  public ResponseEntity<McpInfoResponse> getInfo() {
    return ResponseEntity.ok(
        new McpInfoResponse(
            "explore-ai-mcp-server",
            "1.0.0",
            "AI Explore MCP Server with RAG, Weather, and Chat tools",
            new McpCapabilitiesResponse(true, true, true),
            Map.of(
                "get_weather", "Get current weather for a city",
                "get_forecast", "Get weather forecast",
                "search_knowledge_base", "Search documents in knowledge base",
                "list_documents", "List all documents",
                "ai_chat", "Chat with AI assistant"),
            Map.of(
                "document:///{docId}", "Access document by ID",
                "config:///{key}", "Access configuration values"),
            Map.of(
                "analyze-document", "Generate document analysis prompt",
                "greeting", "Generate greeting message")));
  }
}
