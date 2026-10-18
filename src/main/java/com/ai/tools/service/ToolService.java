package com.ai.tools.service;

import com.ai.common.domain.event.AiInvocationCompletedEvent;
import com.ai.common.domain.event.AiInvocationFailedEvent;
import com.ai.common.domain.model.OwnerKey;
import com.ai.common.service.llm.ChatClientProvider;
import com.ai.common.service.llm.DocumentSearchTool;
import com.ai.common.service.llm.TextChatOptions;
import com.ai.common.service.llm.WebSearchTool;
import com.ai.tools.domain.model.WeatherForecast;
import com.ai.tools.domain.model.WeatherQuery;
import com.ai.tools.domain.model.WeatherSimulator;
import com.ai.tools.infra.tools.WeatherTools;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** Entry point for tool-augmented chat and direct weather, document, and web search calls. */
@Service
@RequiredArgsConstructor
public class ToolService {

  private final ChatClientProvider chatClientProvider;
  private final WeatherTools weatherTools;
  private final WeatherSimulator weatherSimulator;
  private final DocumentSearchTool documentSearchTool;
  private final WebSearchTool webSearchTool;
  private final ApplicationEventPublisher eventPublisher;

  /** Looks up today's weather in the city. */
  public String lookupWeather(String city) {
    return weatherSimulator.lookupCurrentWeather(WeatherQuery.createQuery(city)).getContent();
  }

  /** Returns a formatted weather forecast for the city over the requested number of days. */
  public String lookupForecast(String city, Integer days) {
    return weatherSimulator
        .generateForecast(WeatherForecast.createForecast(WeatherQuery.createQuery(city), days))
        .getContent();
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
          chatClientProvider.createStateless(TextChatOptions.createOptions("openai", null, true));
      String content = chatClient.prompt().user(question).call().content();
      eventPublisher.publishEvent(
          new AiInvocationCompletedEvent(
              "tools",
              "tool.chat",
              elapsedMillis(startedAt),
              owner,
              "openai",
              null,
              null,
              null,
              null));
      return content;
    } catch (RuntimeException ex) {
      eventPublisher.publishEvent(
          new AiInvocationFailedEvent(
              "tools",
              "tool.chat",
              elapsedMillis(startedAt),
              owner,
              "openai",
              null,
              null,
              null,
              null,
              ex.getClass().getSimpleName(),
              ex.getMessage()));
      throw ex;
    }
  }

  /** Searches the web. */
  public String searchWeb(String query) {
    return webSearchTool.searchWeb(query);
  }

  private static long elapsedMillis(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000L;
  }
}
