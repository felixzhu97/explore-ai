package com.ai.mcp.infrastructure.server;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Single source of truth for REST {@code /api/mcp/info} — must match
 * {@link AiMcpServerService} {@code @McpTool}/{@code @McpResource}/{@code @McpPrompt} annotations.
 */
@Component
public class McpServerCapabilityCatalog {

    public static final String SERVER_NAME = "explore-ai-mcp-server";
    public static final String SERVER_VERSION = "1.0.0";
    public static final String SERVER_DESCRIPTION =
            "AI Explore MCP Server with RAG, Weather, Chat tools, resources, and prompts";

    public Map<String, String> tools() {
        Map<String, String> tools = new LinkedHashMap<>();
        tools.put("get_weather", "Get current weather for a city");
        tools.put("get_forecast", "Get weather forecast");
        tools.put("search_knowledge_base", "Search documents in knowledge base");
        tools.put("list_documents", "List all documents");
        tools.put("ai_chat", "Chat with AI assistant");
        return Map.copyOf(tools);
    }

    public Map<String, String> resources() {
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("document:///{docId}", "Access document by ID");
        resources.put("config:///{key}", "Access configuration values");
        return Map.copyOf(resources);
    }

    public Map<String, String> prompts() {
        Map<String, String> prompts = new LinkedHashMap<>();
        prompts.put("analyze-document", "Generate document analysis prompt");
        prompts.put("greeting", "Generate greeting message");
        return Map.copyOf(prompts);
    }

    public Map<String, Object> infoPayload() {
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tools", true);
        capabilities.put("resources", true);
        capabilities.put("prompts", true);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", SERVER_NAME);
        body.put("version", SERVER_VERSION);
        body.put("description", SERVER_DESCRIPTION);
        body.put("capabilities", Map.copyOf(capabilities));
        body.put("availableTools", tools());
        body.put("availableResources", resources());
        body.put("availablePrompts", prompts());
        return Map.copyOf(body);
    }
}
