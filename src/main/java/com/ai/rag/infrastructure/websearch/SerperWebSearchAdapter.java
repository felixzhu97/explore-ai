package com.ai.rag.infrastructure.websearch;

import com.ai.common.domain.repository.WebSearchTool;
import com.ai.common.infrastructure.llm.ToolEventChannel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final int RESULT_COUNT = 8;

    private final RestClient restClient;
    private final String apiKey;

    @Autowired
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
    @Tool(description = """
            Web search tool (在线搜索 / 联网搜索). Call when the user asks to search
            online, or needs live statistics to chart (市场份额, EV share, prices,
            rankings). Also trigger on phrases like 进行在线搜索 / 查一下 / look up.
            Call at most once per user question. Prefer an English query that includes
            year/quarter, region, metric, and brand names even if the user wrote Chinese
            (e.g. "2025 Q1 global EV market share Tesla BYD Volkswagen"). After results
            return, answer immediately with markdown + a2ui chart if requested; do not
            call this tool again. Snippets often map to chartData [{label, value}, ...].""")
    public String searchWeb(@ToolParam(description = "Search query; prefer English with year, region, metric, and brand names") String query) {
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
                    .body(Map.of("q", query, "num", RESULT_COUNT))
                    .retrieve()
                    .body(SerperResponse.class);

            if (response == null) {
                return "No search results found for: " + query;
            }

            boolean hasOrganic = response.organic() != null && !response.organic().isEmpty();
            boolean hasAnswer = response.answerBox() != null;
            boolean hasKnowledge = response.knowledgeGraph() != null;
            if (!hasOrganic && !hasAnswer && !hasKnowledge) {
                return "No search results found for: " + query;
            }

            publishSources(query, response.organic());
            return formatResults(query, response);

        } catch (Exception e) {
            log.error("Web search failed for query: {}", query, e);
            return "Failed to search the web. Please try again later.";
        }
    }

    private void publishSources(String query, List<SerperResponse.OrganicResult> results) {
        List<Map<String, String>> items = new ArrayList<>();
        if (results != null) {
            for (SerperResponse.OrganicResult result : results) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("title", result.title() == null ? "" : result.title());
                item.put("url", result.link() == null ? "" : result.link());
                item.put("snippet", result.snippet() == null ? "" : result.snippet());
                items.add(item);
            }
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

    private String formatResults(String query, SerperResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("Web search results for: ").append(query).append("\n\n");

        if (response.answerBox() != null) {
            AnswerBox box = response.answerBox();
            sb.append("Answer box:\n");
            if (box.title() != null) {
                sb.append("Title: ").append(box.title()).append('\n');
            }
            if (box.answer() != null) {
                sb.append("Answer: ").append(box.answer()).append('\n');
            }
            if (box.snippet() != null) {
                sb.append("Snippet: ").append(box.snippet()).append('\n');
            }
            if (box.link() != null) {
                sb.append("URL: ").append(box.link()).append('\n');
            }
            sb.append('\n');
        }

        if (response.knowledgeGraph() != null) {
            KnowledgeGraph graph = response.knowledgeGraph();
            sb.append("Knowledge graph:\n");
            if (graph.title() != null) {
                sb.append("Title: ").append(graph.title()).append('\n');
            }
            if (graph.type() != null) {
                sb.append("Type: ").append(graph.type()).append('\n');
            }
            if (graph.description() != null) {
                sb.append("Description: ").append(graph.description()).append('\n');
            }
            if (graph.attributes() != null && !graph.attributes().isEmpty()) {
                sb.append("Attributes:\n");
                graph.attributes().forEach((key, value) ->
                        sb.append("  - ").append(key).append(": ").append(value).append('\n'));
            }
            sb.append('\n');
        }

        if (response.organic() != null) {
            for (int i = 0; i < response.organic().size(); i++) {
                SerperResponse.OrganicResult result = response.organic().get(i);
                sb.append(String.format("[%d] %s\n", i + 1, result.title()));
                sb.append(String.format("URL: %s\n", result.link()));
                if (result.snippet() != null) {
                    sb.append(String.format("Summary: %s\n", result.snippet()));
                }
                sb.append('\n');
            }

            sb.append("Sources:\n");
            for (SerperResponse.OrganicResult result : response.organic()) {
                sb.append(String.format("- %s\n", result.link()));
            }
        }

        return sb.toString();
    }

    record SerperResponse(
            List<OrganicResult> organic,
            AnswerBox answerBox,
            KnowledgeGraph knowledgeGraph
    ) {
        record OrganicResult(
                String title,
                String link,
                String snippet,
                String displayedLink,
                List<String> snippetHighlighting
        ) {}
    }

    record AnswerBox(String title, String answer, String snippet, String link) {}

    record KnowledgeGraph(
            String title,
            String type,
            String description,
            Map<String, Object> attributes
    ) {}
}
