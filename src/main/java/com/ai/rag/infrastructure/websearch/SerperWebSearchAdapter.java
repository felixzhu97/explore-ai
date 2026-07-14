package com.ai.rag.infrastructure.websearch;

import com.ai.common.domain.repository.WebSearchTool;
import com.ai.common.infrastructure.llm.ToolEventChannel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serper.dev web search tool implementation.
 * Provides real-time web search capabilities via Google SERP.
 */
@Component
public class SerperWebSearchAdapter implements WebSearchTool {

    private static final Logger log = LoggerFactory.getLogger(SerperWebSearchAdapter.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient restClient;
    private final String apiKey;

    public SerperWebSearchAdapter(
            @Value("${serper.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://google.serper.dev")
                .defaultHeaders(headers -> {
                    headers.set("X-API-KEY", apiKey);
                    headers.set("Content-Type", "application/json");
                })
                .build();
    }

    SerperWebSearchAdapter(String apiKey, RestClient restClient) {
        this.apiKey = apiKey;
        this.restClient = restClient;
    }

    @Override
    @Tool(description = "Search the web for current information, real-time news, and live data. Use this when user asks about current events, recent news, weather, stock prices, or any topic requiring up-to-date information from the internet.")
    public String searchWeb(@ToolParam(description = "The search query to find current information") String query) {
        if (query == null || query.isBlank()) {
            return "Please provide a valid search query.";
        }

        log.info("Web search query: {}", query);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Serper API key not configured");
            return "Web search is not available. Please configure SERPER_API_KEY.";
        }

        try {
            var response = restClient.post()
                    .uri("/search")
                    .body(Map.of("q", query, "num", 5))
                    .retrieve()
                    .body(SerperResponse.class);

            if (response == null || response.organic() == null || response.organic().isEmpty()) {
                return "No search results found for: " + query;
            }

            publishSources(query, response.organic());
            return formatResults(query, response.organic());

        } catch (Exception e) {
            log.error("Web search failed for query: {}", query, e);
            return "Failed to search the web. Please try again later.";
        }
    }

    private void publishSources(String query, List<SerperResponse.OrganicResult> results) {
        List<Map<String, String>> items = new ArrayList<>();
        for (SerperResponse.OrganicResult result : results) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("title", result.title() == null ? "" : result.title());
            item.put("url", result.link() == null ? "" : result.link());
            item.put("snippet", result.snippet() == null ? "" : result.snippet());
            items.add(item);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "sources");
        payload.put("query", query);
        payload.put("items", items);
        try {
            ToolEventChannel.publish(JSON.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("Failed to publish search sources event", e);
        }
    }

    private String formatResults(String query, List<SerperResponse.OrganicResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Web search results for: ").append(query).append("\n\n");

        for (int i = 0; i < results.size(); i++) {
            SerperResponse.OrganicResult result = results.get(i);
            sb.append(String.format("[%d] %s\n", i + 1, result.title()));
            sb.append(String.format("URL: %s\n", result.link()));
            if (result.snippet() != null) {
                sb.append(String.format("Summary: %s\n", result.snippet()));
            }
            sb.append("\n");
        }

        sb.append("Sources:\n");
        for (SerperResponse.OrganicResult result : results) {
            sb.append(String.format("- %s\n", result.link()));
        }

        return sb.toString();
    }

    record SerperResponse(List<OrganicResult> organic) {
        record OrganicResult(
                String title,
                String link,
                String snippet,
                String displayedLink,
                List<String> snippetHighlighting
        ) {}
    }
}
