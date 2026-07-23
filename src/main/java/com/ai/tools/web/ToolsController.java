package com.ai.tools.web;

import com.ai.tools.application.usecase.ToolsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tools REST Controller for weather and document search.
 */
@RestController
@RequestMapping("/api")
public class ToolsController {

    private static final Logger log = LoggerFactory.getLogger(ToolsController.class);

    private final ToolsFacade toolsFacade;

    public ToolsController(ToolsFacade toolsFacade) {
        this.toolsFacade = toolsFacade;
    }

    /**
     * Get weather for a city.
     */
    @GetMapping("/tools/weather")
    public ResponseEntity<String> getWeather(@RequestParam(required = false) String city) {
        if (city == null || city.isBlank()) {
            return ResponseEntity.badRequest().body("城市参数不能为空");
        }
        try {
            return ResponseEntity.ok(toolsFacade.getWeather(city));
        } catch (Exception e) {
            log.error("Error fetching weather for {}", city, e);
            return ResponseEntity.internalServerError().body("获取天气信息失败");
        }
    }

    /**
     * Get weather forecast.
     */
    @GetMapping("/tools/weather/forecast")
    public ResponseEntity<String> getForecast(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer days) {
        if (city == null || city.isBlank()) {
            return ResponseEntity.badRequest().body("城市参数不能为空");
        }
        try {
            return ResponseEntity.ok(toolsFacade.getForecast(city, days));
        } catch (Exception e) {
            log.error("Error fetching forecast for {}", city, e);
            return ResponseEntity.internalServerError().body("获取天气预报失败");
        }
    }

    /**
     * Search documents in knowledge base.
     */
    @GetMapping("/tools/documents/search")
    public ResponseEntity<String> searchDocuments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String docIds) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body("搜索关键词不能为空");
        }
        try {
            List<String> docIdList = null;
            if (docIds != null && !docIds.isBlank()) {
                docIdList = List.of(docIds.split(","));
            }
            return ResponseEntity.ok(toolsFacade.searchDocuments(query, docIdList));
        } catch (Exception e) {
            log.error("Error searching documents", e);
            return ResponseEntity.internalServerError().body("搜索文档失败");
        }
    }

    /**
     * List all documents in knowledge base.
     */
    @GetMapping("/tools/documents/list")
    public ResponseEntity<String> listDocuments() {
        try {
            return ResponseEntity.ok(toolsFacade.listDocuments());
        } catch (Exception e) {
            log.error("Error listing documents", e);
            return ResponseEntity.internalServerError().body("获取文档列表失败");
        }
    }

    /**
     * List tools available for Chat tool-calling (local + MCP).
     */
    @GetMapping("/tools/catalog")
    public ResponseEntity<List<ToolCatalogResponse>> listCatalog() {
        try {
            List<ToolCatalogResponse> body = toolsFacade.listCatalog().stream()
                    .map(entry -> new ToolCatalogResponse(
                            entry.name(),
                            entry.description(),
                            entry.source().name()))
                    .toList();
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Error listing tool catalog", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Chat with function calling.
     */
    @PostMapping("/tools/chat")
    public ResponseEntity<ToolChatResponse> chatWithTools(@RequestBody ToolChatRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().body(new ToolChatResponse("问题不能为空", null));
        }
        try {
            String response = toolsFacade.chatWithTools(request.question());
            return ResponseEntity.ok(new ToolChatResponse(response, null));
        } catch (Exception e) {
            log.error("Error in chat with tools", e);
            return ResponseEntity.internalServerError().body(new ToolChatResponse("抱歉，处理您的请求时发生错误，请稍后重试。", null));
        }
    }

    public record ToolChatRequest(String question, List<String> docIds) {}

    public record ToolChatResponse(String answer, List<String> toolCalls) {}

    public record ToolCatalogResponse(String name, String description, String source) {}
}
