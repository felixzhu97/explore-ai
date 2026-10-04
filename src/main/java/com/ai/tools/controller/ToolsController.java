package com.ai.tools.controller;

import com.ai.tools.controller.dto.ToolChatRequest;
import com.ai.tools.controller.dto.ToolChatResponse;
import com.ai.tools.service.ToolService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tools REST Controller for weather and document search. */
@RestController
@RequestMapping("/api/tools")
public class ToolsController {

  private static final Logger log = LoggerFactory.getLogger(ToolsController.class);

  private final ToolService toolService;

  public ToolsController(ToolService toolService) {
    this.toolService = toolService;
  }

  /** Get weather for a city. */
  @GetMapping("/weather")
  public ResponseEntity<String> getWeather(@RequestParam(required = false) String city) {
    if (city == null || city.isBlank()) {
      return ResponseEntity.badRequest().body("城市参数不能为空");
    }
    try {
      return ResponseEntity.ok(toolService.lookupWeather(city));
    } catch (Exception e) {
      log.error("Error fetching weather for {}", city, e);
      return ResponseEntity.internalServerError().body("获取天气信息失败");
    }
  }

  /** Get weather forecast. */
  @GetMapping("/weather/forecast")
  public ResponseEntity<String> getForecast(
      @RequestParam(required = false) String city, @RequestParam(required = false) Integer days) {
    if (city == null || city.isBlank()) {
      return ResponseEntity.badRequest().body("城市参数不能为空");
    }
    try {
      return ResponseEntity.ok(toolService.lookupForecast(city, days));
    } catch (Exception e) {
      log.error("Error fetching forecast for {}", city, e);
      return ResponseEntity.internalServerError().body("获取天气预报失败");
    }
  }

  /** Search documents in knowledge base. */
  @GetMapping("/documents/search")
  public ResponseEntity<String> searchDocuments(
      @RequestParam(required = false) String query, @RequestParam(required = false) String docIds) {
    if (query == null || query.isBlank()) {
      return ResponseEntity.badRequest().body("搜索关键词不能为空");
    }
    try {
      List<String> docIdList = null;
      if (docIds != null && !docIds.isBlank()) {
        docIdList = List.of(docIds.split(","));
      }
      return ResponseEntity.ok(toolService.searchDocuments(query, docIdList));
    } catch (Exception e) {
      log.error("Error searching documents", e);
      return ResponseEntity.internalServerError().body("搜索文档失败");
    }
  }

  /** List all documents in knowledge base. */
  @GetMapping("/documents")
  public ResponseEntity<String> listDocuments() {
    try {
      return ResponseEntity.ok(toolService.listDocuments());
    } catch (Exception e) {
      log.error("Error listing documents", e);
      return ResponseEntity.internalServerError().body("获取文档列表失败");
    }
  }

  /** Chat with function calling. */
  @PostMapping("/chat")
  public ResponseEntity<ToolChatResponse> chatWithTools(@RequestBody ToolChatRequest request) {
    if (request == null || request.question() == null || request.question().isBlank()) {
      return ResponseEntity.badRequest().body(new ToolChatResponse("问题不能为空", null));
    }
    try {
      String response = toolService.chatWithTools(request.question());
      return ResponseEntity.ok(new ToolChatResponse(response, null));
    } catch (Exception e) {
      log.error("Error in chat with tools", e);
      return ResponseEntity.internalServerError()
          .body(new ToolChatResponse("抱歉，处理您的请求时发生错误，请稍后重试。", null));
    }
  }
}
