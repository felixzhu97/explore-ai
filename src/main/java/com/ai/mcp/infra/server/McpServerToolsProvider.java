package com.ai.mcp.infra.server;

import com.ai.chat.service.ChatService;
import com.ai.common.domain.tool.DocumentSearchTool;
import com.ai.common.infra.logging.LogSanitizer;
import com.ai.rag.infra.config.RagProperties;
import com.ai.tools.infra.tools.WeatherTools;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** MCP server endpoint exposing weather, knowledge-base, chat tools, and a RAG config resource. */
@Component
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-mcp",
    havingValue = "true",
    matchIfMissing = false)
public class McpServerToolsProvider {

  private static final Logger log = LoggerFactory.getLogger(McpServerToolsProvider.class);

  private final WeatherTools weatherTools;
  private final DocumentSearchTool documentSearchTool;
  private final ChatService chatService;
  private final RagProperties ragProperties;

  public McpServerToolsProvider(
      WeatherTools weatherTools,
      DocumentSearchTool documentSearchTool,
      ChatService chatService,
      RagProperties ragProperties) {
    this.weatherTools = weatherTools;
    this.documentSearchTool = documentSearchTool;
    this.chatService = chatService;
    this.ragProperties = ragProperties;
  }

  @McpTool(
      name = "get_weather",
      description = "Get current weather information for a specified city")
  public String getWeather(
      @McpToolParam(description = "The city name to get weather for", required = true)
          String city) {
    log.info("MCP tool: getWeather called for city: {}", city);
    return weatherTools.getWeather(city);
  }

  @McpTool(name = "get_forecast", description = "Get weather forecast for a specified city")
  public String getForecast(
      @McpToolParam(description = "The city name", required = true) String city,
      @McpToolParam(description = "Number of days for forecast (default: 3)", required = false)
          Integer days) {
    log.info("MCP tool: getForecast called for city: {} with {} days", city, days);
    return weatherTools.getForecast(city, days);
  }

  @McpTool(
      name = "search_knowledge_base",
      description = "Search documents in the knowledge base using semantic search")
  public String searchKnowledgeBase(
      @McpToolParam(description = "The search query", required = true) String query,
      @McpToolParam(
              description = "Optional document IDs to filter (comma-separated)",
              required = false)
          String docIds) {
    log.info("MCP tool: searchKnowledgeBase called with query: {}", query);

    List<String> docIdList = null;
    if (docIds != null && !docIds.isBlank()) {
      docIdList = List.of(docIds.split(","));
    }

    return documentSearchTool.searchDocuments(query, docIdList);
  }

  @McpTool(
      name = "list_documents",
      description = "List all documents available in the knowledge base")
  public String listDocuments() {
    log.info("MCP tool: listDocuments called");
    return documentSearchTool.listDocuments();
  }

  @McpTool(name = "ai_chat", description = "Chat with AI assistant")
  public String aiChat(
      @McpToolParam(description = "The message to send to the AI", required = true)
          String message) {
    log.info("MCP tool: aiChat called with message: {}", LogSanitizer.truncate(message, 50));
    return chatService.chat(message);
  }

  /** Returns the value of a supported RAG chunking or retrieval setting by property key. */
  @McpResource(
      uri = "config:///{key}",
      name = "Configuration Resource",
      description = "Access application configuration")
  public String getConfig(String key) {
    log.info("MCP resource: getConfig called for key: {}", key);

    return switch (key) {
      case "spring.ai.rag.chunk.size" -> String.valueOf(ragProperties.getChunk().getSize());
      case "spring.ai.rag.chunk.overlap" -> String.valueOf(ragProperties.getChunk().getOverlap());
      case "spring.ai.rag.retrieval.top-k" ->
          String.valueOf(ragProperties.getRetrieval().getTopK());
      case "spring.ai.rag.retrieval.score-threshold" ->
          String.valueOf(ragProperties.getRetrieval().getScoreThreshold());
      default -> "Configuration key not found: " + key;
    };
  }
}
