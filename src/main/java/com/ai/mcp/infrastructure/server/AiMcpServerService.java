package com.ai.mcp.infrastructure.server;

import com.ai.common.domain.port.out.DocumentSearchTool;
import com.ai.chat.application.usecase.ChatUseCase;
import com.ai.tools.infrastructure.tools.WeatherTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiMcpServerService {

    private static final Logger log = LoggerFactory.getLogger(AiMcpServerService.class);

    private final WeatherTools weatherTools;
    private final DocumentSearchTool documentSearchTool;
    private final ChatUseCase aiChatUseCase;

    public AiMcpServerService(WeatherTools weatherTools, DocumentSearchTool documentSearchTool, ChatUseCase aiChatUseCase) {
        this.weatherTools = weatherTools;
        this.documentSearchTool = documentSearchTool;
        this.aiChatUseCase = aiChatUseCase;
    }

    @McpTool(name = "get_weather", description = "Get current weather information for a specified city")
    public String getWeather(
            @McpToolParam(description = "The city name to get weather for", required = true) String city) {
        log.info("MCP tool: getWeather called for city: {}", city);
        return weatherTools.getWeather(city);
    }

    @McpTool(name = "get_forecast", description = "Get weather forecast for a specified city")
    public String getForecast(
            @McpToolParam(description = "The city name", required = true) String city,
            @McpToolParam(description = "Number of days for forecast (default: 3)", required = false) Integer days) {
        log.info("MCP tool: getForecast called for city: {} with {} days", city, days);
        return weatherTools.getForecast(city, days);
    }

    @McpTool(name = "search_knowledge_base", description = "Search documents in the knowledge base using semantic search")
    public String searchKnowledgeBase(
            @McpToolParam(description = "The search query", required = true) String query,
            @McpToolParam(description = "Optional document IDs to filter (comma-separated)", required = false) String docIds) {
        log.info("MCP tool: searchKnowledgeBase called with query: {}", query);

        List<String> docIdList = null;
        if (docIds != null && !docIds.isBlank()) {
            docIdList = List.of(docIds.split(","));
        }

        return documentSearchTool.searchDocuments(query, docIdList);
    }

    @McpTool(name = "list_documents", description = "List all documents available in the knowledge base")
    public String listDocuments() {
        log.info("MCP tool: listDocuments called");
        return documentSearchTool.listDocuments();
    }

    @McpTool(name = "ai_chat", description = "Chat with AI assistant")
    public String aiChat(
            @McpToolParam(description = "The message to send to the AI", required = true) String message) {
        log.info("MCP tool: aiChat called with message: {}", truncate(message, 50));
        return aiChatUseCase.chat(message);
    }

    @McpResource(uri = "config:///{key}", name = "Configuration Resource", description = "Access application configuration")
    public String getConfig(String key) {
        log.info("MCP resource: getConfig called for key: {}", key);

        return switch (key) {
            case "spring.ai.rag.chunk.size" -> "500";
            case "spring.ai.rag.chunk.overlap" -> "50";
            case "spring.ai.rag.retrieval.top-k" -> "5";
            case "spring.ai.rag.retrieval.score-threshold" -> "0.5";
            default -> "Configuration key not found: " + key;
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
