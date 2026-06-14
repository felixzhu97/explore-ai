package com.ai.infrastructure.adapter.tool.web;

import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.infrastructure.adapter.tool.ToolProvider;
import com.ai.infrastructure.adapter.web.WebSearchPort;
import com.ai.infrastructure.adapter.web.WebSearchPort.WebSearchException;
import com.ai.infrastructure.adapter.web.WebSearchResult;
import com.ai.infrastructure.config.WebSearchProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool for web search using DuckDuckGo.
 * Returns search results with title, URL, and snippet.
 */
@Component
public class WebSearchTool implements ToolProvider {

    private final WebSearchPort webSearchPort;
    private final WebSearchProperties webSearchProperties;

    public WebSearchTool(WebSearchPort webSearchPort, WebSearchProperties webSearchProperties) {
        this.webSearchPort = webSearchPort;
        this.webSearchProperties = webSearchProperties;
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.atomic(
            "web_search",
            "Search the web using DuckDuckGo. Returns a list of search results with title, URL, and snippet. Use this when you need current information or answers to questions that require up-to-date knowledge.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of(
                        "type", "string",
                        "description", "The search query"
                    ),
                    "maxResults", Map.of(
                        "type", "integer",
                        "description", "Maximum number of results to return (1-20, default " + 5 + ")",
                        "minimum", 1,
                        "maximum", 20
                    ),
                    "region", Map.of(
                        "type", "string",
                        "description", "Search region code (e.g., 'us-en' for US English, 'cn-zh' for Chinese). Default: 'us-en'"
                    )
                ),
                "required", List.of("query")
            ),
            "web"
        );
    }

    @Override
    public ToolExecutor executor() {
        return this::execute;
    }

    public ToolResult execute(ToolInvocation invocation) {
        try {
            String query = invocation.getArg("query", "");
            if (query.isEmpty()) {
                return ToolResult.error("Query is required");
            }

            Integer maxResultsArg = invocation.getArg("maxResults", null);
            int maxResults = maxResultsArg != null
                ? Math.max(1, Math.min(20, maxResultsArg))
                : webSearchProperties.getDefaultMaxResults();

            String region = invocation.getArg("region", webSearchProperties.getDefaultRegion());
            if (region == null || region.isEmpty()) {
                region = webSearchProperties.getDefaultRegion();
            }

            List<WebSearchResult> results = webSearchPort.search(query, maxResults, region);

            StringBuilder content = new StringBuilder();
            content.append("# Web Search Results\n\n");
            content.append("**Query:** ").append(query).append("\n");
            content.append("**Region:** ").append(region).append("\n");
            content.append("**Results:** ").append(results.size()).append("\n\n");

            if (results.isEmpty()) {
                content.append("_No results found for this query._\n");
            } else {
                content.append("---\n\n");
                for (WebSearchResult result : results) {
                    content.append("### ").append(result.rank() + 1).append(". ").append(result.title()).append("\n\n");
                    content.append("**URL:** ").append(result.url()).append("\n\n");
                    content.append(result.snippet()).append("\n\n");
                    content.append("---\n\n");
                }
            }

            List<Map<String, Object>> structuredResults = results.stream()
                .map(r -> Map.<String, Object>of(
                    "title", r.title(),
                    "url", r.url(),
                    "snippet", r.snippet(),
                    "rank", r.rank()
                ))
                .toList();

            Map<String, Object> structured = Map.of(
                "query", query,
                "region", region,
                "results", structuredResults
            );

            return ToolResult.success(content.toString(), structured);

        } catch (WebSearchException e) {
            if (e.isRateLimited()) {
                return ToolResult.error("DuckDuckGo rate limit exceeded. Please try again later.");
            }
            return ToolResult.error("Web search failed: " + e.getMessage());
        } catch (Exception e) {
            return ToolResult.error("Web search failed: " + e.getMessage());
        }
    }
}
