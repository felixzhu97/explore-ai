package com.ai.tools.service;

import com.ai.common.domain.tool.DocumentSearchTool;
import com.ai.common.domain.tool.WebSearchTool;
import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.metrics.domain.vo.AiDomain;
import com.ai.metrics.domain.vo.Latency;
import com.ai.metrics.service.AiInvocationRecorder;
import com.ai.tools.domain.model.WeatherReport;
import com.ai.tools.domain.vo.WeatherForecast;
import com.ai.tools.domain.vo.WeatherQuery;
import com.ai.tools.infra.tools.WeatherTools;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** Entry point for tool-augmented chat and direct weather, document, and web search calls. */
@Service
@RequiredArgsConstructor
public class ToolService {

  private final ChatClientProvider chatClientProvider;
  private final WeatherTools weatherTools;
  private final WeatherReport weatherReport;
  private final DocumentSearchTool documentSearchTool;
  private final WebSearchTool webSearchTool;
  private final AiInvocationRecorder invocationRecorder;

  /** Looks up today's weather in the city. */
  public String lookupWeather(String city) {
    return weatherReport.lookupCurrent(WeatherQuery.of(city)).content();
  }

  /** Returns a formatted weather forecast for the city over the requested number of days. */
  public String lookupForecast(String city, Integer days) {
    return weatherReport
        .generateForecast(WeatherForecast.of(WeatherQuery.of(city), days))
        .content();
  }

  /** Searches the uploaded documents. */
  public String searchDocuments(String query, List<String> documentIds) {
    return documentSearchTool.searchDocuments(query, documentIds);
  }

  /** Lists the uploaded documents. */
  public String listDocuments() {
    return documentSearchTool.listDocuments();
  }

  /** Answers the question via a tool-enabled OpenAI chat client and records the invocation. */
  public String chatWithTools(String question, OwnerKey owner) {
    long startedAt = System.nanoTime();
    try {
      ChatClient chatClient =
          chatClientProvider.createStateless(TextChatOptions.of("openai", null, true));
      String content = chatClient.prompt().user(question).call().content();
      invocationRecorder.recordSuccess(
          AiDomain.TOOLS, "tool.chat", Latency.since(startedAt), owner, "openai", null, null);
      return content;
    } catch (RuntimeException ex) {
      invocationRecorder.recordError(
          AiDomain.TOOLS, "tool.chat", Latency.since(startedAt), owner, "openai", null, null, ex);
      throw ex;
    }
  }

  /** Searches the web. */
  public String searchWeb(String query) {
    return webSearchTool.searchWeb(query);
  }
}
