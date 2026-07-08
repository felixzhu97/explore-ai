package com.ai.mcp.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for MCP Server management endpoints.
 */
@RestController
@RequestMapping("/api/mcp")
@Tag(name = "MCP Server", description = "MCP Server management")
public class McpController {

    @GetMapping("/health")
    @Operation(summary = "MCP Server health check")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "server", "explore-ai-mcp-server",
                "version", "1.0.0",
                "protocol", "MCP 1.0"
        ));
    }

    @GetMapping("/info")
    @Operation(summary = "Get MCP Server information")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
                "name", "explore-ai-mcp-server",
                "version", "1.0.0",
                "description", "AI Explore MCP Server with RAG, Weather, and Chat tools",
                "capabilities", Map.of(
                        "tools", true,
                        "resources", true,
                        "prompts", true
                ),
                "availableTools", Map.of(
                        "get_weather", "Get current weather for a city",
                        "get_forecast", "Get weather forecast",
                        "search_knowledge_base", "Search documents in knowledge base",
                        "list_documents", "List all documents",
                        "ai_chat", "Chat with AI assistant"
                ),
                "availableResources", Map.of(
                        "document:///{docId}", "Access document by ID",
                        "config:///{key}", "Access configuration values"
                ),
                "availablePrompts", Map.of(
                        "analyze-document", "Generate document analysis prompt",
                        "greeting", "Generate greeting message"
                )
        ));
    }
}
