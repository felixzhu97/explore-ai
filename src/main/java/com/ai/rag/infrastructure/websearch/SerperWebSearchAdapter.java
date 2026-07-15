package com.ai.rag.infrastructure.websearch;

import com.ai.common.domain.repository.WebSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Serper.dev web search tool implementation.
 * Provides real-time web search capabilities via Google SERP.
 */
@Component
public class SerperWebSearchAdapter implements WebSearchTool {

    private static final Logger log = LoggerFactory.getLogger(SerperWebSearchAdapter.class);
    private static final int RESULT_COUNT = 8;

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

    @Override
    @Tool(description = """
            Search the web for current information, statistics, and live data.
            Call at most once per user question. After results return, answer
            immediately (including any a2ui chart) without calling this tool again.
            Use for up-to-date numbers to chart or compare (market share, population,
            prices, rankings, election results, GDP, etc.), or current events / news.
            Prefer focused queries that include year, region, and metric. Snippets
            often contain figures you can map to chartData [{label, value}, ...].""")
    public String searchWeb(@ToolParam(description = "Focused search query including metric, year, and region when possible") String query) {
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

            return formatResults(query, response);

        } catch (Exception e) {
            log.error("Web search failed for query: {}", query, e);
            return "Failed to search the web. Please try again later.";
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
