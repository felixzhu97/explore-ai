package com.ai.tools.service;

import com.ai.common.domain.tool.DocumentSearchTool;
import com.ai.common.domain.tool.WebSearchTool;
import com.ai.common.infra.logging.LogSanitizer;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.service.AiInvocationRecorder;
import com.ai.tools.domain.model.WeatherReport;
import com.ai.tools.domain.vo.WeatherForecast;
import com.ai.tools.domain.vo.WeatherQuery;
import com.ai.tools.infra.tools.WeatherTools;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** Entry point for tool-augmented chat and direct weather, document, and web search calls. */
@Service
public class ToolService {

  private static final Logger log = LoggerFactory.getLogger(ToolService.class);

  private final ChatClientProvider chatClientProvider;
  private final WeatherTools weatherTools;
  private final WeatherReport weatherReport;
  private final DocumentSearchTool documentSearchTool;
  private final WebSearchTool webSearchTool;
  private final AiInvocationRecorder invocationRecorder;

  public ToolService(
      ChatClientProvider chatClientProvider,
      WeatherTools weatherTools,
      WeatherReport weatherReport,
      DocumentSearchTool documentSearchTool,
      WebSearchTool webSearchTool,
      AiInvocationRecorder invocationRecorder) {
    this.chatClientProvider = chatClientProvider;
    this.weatherTools = weatherTools;
    this.weatherReport = weatherReport;
    this.documentSearchTool = documentSearchTool;
    this.webSearchTool = webSearchTool;
    this.invocationRecorder = invocationRecorder;
  }

  /** Answers the question via a tool-enabled OpenAI chat client and records the invocation. */
  public String chatWithTools(String question) {
    log.info("ToolService.chatWithTools: {}", LogSanitizer.truncate(question));
    long startedAt = System.nanoTime();
    try {
      ChatClient chatClient =
          chatClientProvider.createStateless(TextChatOptions.of("openai", null, true));
      String content = chatClient.prompt().user(question).call().content();
      invocationRecorder.recordSuccess(
          AiDomain.TOOLS,
          "tool.chat",
          (System.nanoTime() - startedAt) / 1_000_000L,
          "openai",
          null,
          null);
      return content;
    } catch (RuntimeException ex) {
      invocationRecorder.recordError(
          AiDomain.TOOLS,
          "tool.chat",
          (System.nanoTime() - startedAt) / 1_000_000L,
          "openai",
          null,
          null,
          ex.getClass().getSimpleName(),
          ex.getMessage());
      throw ex;
    }
  }

  public String lookupWeather(String city) {
    log.info("ToolService.lookupWeather: {}", city);
    return weatherReport.lookupCurrent(WeatherQuery.of(city)).content();
  }

  /** Returns a formatted weather forecast for the city over the requested number of days. */
  public String lookupForecast(String city, Integer days) {
    log.info("ToolService.lookupForecast: {} days={}", city, days);
    return weatherReport
        .generateForecast(WeatherForecast.of(WeatherQuery.of(city), days))
        .content();
  }

  public String searchDocuments(String query, List<String> documentIds) {
    log.info("ToolService.searchDocuments: {}", LogSanitizer.truncate(query));
    return documentSearchTool.searchDocuments(query, documentIds);
  }

  public String listDocuments() {
    log.info("ToolService.listDocuments");
    return documentSearchTool.listDocuments();
  }

  public String searchWeb(String query) {
    log.info("ToolService.searchWeb: {}", LogSanitizer.truncate(query));
    return webSearchTool.searchWeb(query);
  }
}
