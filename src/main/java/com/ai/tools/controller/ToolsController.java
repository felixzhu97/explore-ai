package com.ai.tools.controller;

import com.ai.tools.controller.dto.ToolChatRequest;
import com.ai.tools.controller.dto.ToolChatResponse;
import com.ai.tools.service.ToolService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
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

  private final ToolService toolService;

  public ToolsController(ToolService toolService) {
    this.toolService = toolService;
  }

  /** Get weather for a city. */
  @GetMapping("/weather")
  public String getWeather(@RequestParam @NotBlank String city) {
    return toolService.lookupWeather(city);
  }

  /** Get weather forecast. */
  @GetMapping("/weather/forecast")
  public String getForecast(
      @RequestParam @NotBlank String city, @RequestParam(required = false) Integer days) {
    return toolService.lookupForecast(city, days);
  }

  /** Search documents in knowledge base. */
  @GetMapping("/documents/search")
  public String searchDocuments(
      @RequestParam @NotBlank String query,
      @RequestParam(required = false) List<String> documentIds) {
    return toolService.searchDocuments(query, documentIds);
  }

  /** List all documents in knowledge base. */
  @GetMapping("/documents")
  public String listDocuments() {
    return toolService.listDocuments();
  }

  /** Chat with function calling. */
  @PostMapping("/chat")
  public ToolChatResponse chatWithTools(@Valid @RequestBody ToolChatRequest request) {
    return new ToolChatResponse(toolService.chatWithTools(request.question()), null);
  }
}
