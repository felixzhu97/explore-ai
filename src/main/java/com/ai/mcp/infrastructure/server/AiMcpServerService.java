package com.ai.mcp.infrastructure.server;

import com.ai.chat.application.usecase.ChatUseCase;
import com.ai.common.domain.repository.DocumentSearchTool;
import com.ai.rag.infrastructure.config.RagProperties;
import com.ai.tools.infrastructure.tools.WeatherTools;
import com.ai.common.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
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
    private final RagProperties ragProperties;

    public AiMcpServerService(
            WeatherTools weatherTools,
            DocumentSearchTool documentSearchTool,
            ChatUseCase aiChatUseCase,
            RagProperties ragProperties) {
        this.weatherTools = weatherTools;
        this.documentSearchTool = documentSearchTool;
        this.aiChatUseCase = aiChatUseCase;
        this.ragProperties = ragProperties;
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
        log.info("MCP tool: aiChat called with message: {}", LogSanitizer.truncate(message, 50));
        return aiChatUseCase.chat(message);
    }

    @McpResource(uri = "document:///{docId}", name = "Document Resource", description = "Access document by ID")
    public String getDocument(String docId) {
        log.info("MCP resource: getDocument called for docId: {}", docId);
        if (docId == null || docId.isBlank()) {
            return "Document id must not be blank";
        }
        String listing = documentSearchTool.listDocuments();
        if (listing == null || listing.isBlank() || "[]".equals(listing.strip())) {
            return "Document not found: " + docId;
        }
        if (!listing.contains(docId)) {
            return "Document not found: " + docId;
        }
        String content = documentSearchTool.searchDocuments("document " + docId, List.of(docId));
        if (content == null || content.isBlank() || "[]".equals(content.strip())) {
            return "Document found but has no searchable content: " + docId;
        }
        return content;
    }

    @McpResource(uri = "config:///{key}", name = "Configuration Resource", description = "Access application configuration")
    public String getConfig(String key) {
        log.info("MCP resource: getConfig called for key: {}", key);

        return switch (key) {
            case "spring.ai.rag.chunk.size" -> String.valueOf(ragProperties.getChunk().getSize());
            case "spring.ai.rag.chunk.overlap" -> String.valueOf(ragProperties.getChunk().getOverlap());
            case "spring.ai.rag.retrieval.top-k" -> String.valueOf(ragProperties.getRetrieval().getTopK());
            case "spring.ai.rag.retrieval.score-threshold" -> String.valueOf(ragProperties.getRetrieval().getScoreThreshold());
            default -> "Configuration key not found: " + key;
        };
    }

    @McpPrompt(name = "analyze-document", description = "Generate document analysis prompt")
    public String analyzeDocumentPrompt(
            @McpArg(name = "docId", description = "Document ID to analyze", required = true) String docId,
            @McpArg(name = "focus", description = "Optional analysis focus", required = false) String focus) {
        log.info("MCP prompt: analyze-document for docId={}", docId);
        String focusLine = (focus == null || focus.isBlank())
                ? "Provide a clear summary, key points, and open questions."
                : "Focus on: " + focus;
        return """
                Analyze the knowledge-base document with id "%s".
                %s
                Use search_knowledge_base or the document:///%s resource when you need content.
                """.formatted(docId, focusLine, docId).strip();
    }

    @McpPrompt(name = "greeting", description = "Generate greeting message")
    public String greetingPrompt(
            @McpArg(name = "name", description = "Name to greet", required = false) String name) {
        log.info("MCP prompt: greeting name={}", name);
        String who = (name == null || name.isBlank()) ? "there" : name.trim();
        return "Write a short, friendly greeting for %s about exploring AI tools on this platform."
                .formatted(who);
    }

}
