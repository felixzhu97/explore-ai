package com.ai.infrastructure.adapter.web;

import com.ai.infrastructure.config.WebSearchProperties;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * DuckDuckGo HTML search adapter using Jsoup for parsing.
 * Hits the HTML lite endpoint which doesn't require an API key.
 */
@Component
public class DuckDuckGoWebSearchAdapter implements WebSearchPort {

    private static final Logger log = LoggerFactory.getLogger(DuckDuckGoWebSearchAdapter.class);

    private final WebSearchProperties properties;
    private final HttpClient httpClient;

    public DuckDuckGoWebSearchAdapter(WebSearchProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .build();
    }

    @Override
    public List<WebSearchResult> search(String query, int maxResults, String region) throws WebSearchException {
        if (query == null || query.trim().isEmpty()) {
            throw new WebSearchException("Query cannot be empty");
        }

        try {
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String url = properties.getBaseUrl() + "?q=" + encodedQuery + "&kl=" + region;

            log.debug("Searching DuckDuckGo: {} (region={}, maxResults={})", query, region, maxResults);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "text/html,application/xhtml+xml")
                .timeout(Duration.ofMillis(properties.getTimeoutMs()))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            if (statusCode == 429) {
                throw new WebSearchException("DuckDuckGo rate limit exceeded. Please try again later.", 429);
            }
            if (statusCode != 200) {
                throw new WebSearchException("DuckDuckGo returned HTTP " + statusCode, statusCode);
            }

            return parseResults(response.body(), maxResults);

        } catch (WebSearchException e) {
            throw e;
        } catch (IOException e) {
            log.error("Network error during search: {}", e.getMessage());
            throw new WebSearchException("Network error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WebSearchException("Search interrupted", e);
        } catch (Exception e) {
            log.error("Unexpected error during search: {}", e.getMessage());
            throw new WebSearchException("Search failed: " + e.getMessage(), e);
        }
    }

    private List<WebSearchResult> parseResults(String html, int maxResults) throws WebSearchException {
        try {
            Document doc = Jsoup.parse(html);
            Elements resultElements = doc.select(".result");

            List<WebSearchResult> results = new ArrayList<>();
            int rank = 0;

            for (Element element : resultElements) {
                if (rank >= maxResults) {
                    break;
                }

                Element linkElement = element.selectFirst(".result__a");
                Element snippetElement = element.selectFirst(".result__snippet");

                if (linkElement != null) {
                    String title = linkElement.text();
                    String url = linkElement.attr("href");

                    // Skip empty or invalid results
                    if (title.isEmpty() || url.isEmpty() || !url.startsWith("http")) {
                        continue;
                    }

                    String snippet = snippetElement != null ? snippetElement.text() : "";

                    results.add(new WebSearchResult(title, url, snippet, rank));
                    rank++;
                }
            }

            log.debug("Parsed {} search results", results.size());
            return results;

        } catch (Exception e) {
            log.error("Failed to parse DuckDuckGo HTML response: {}", e.getMessage());
            throw new WebSearchException("Failed to parse search results: " + e.getMessage(), e);
        }
    }
}
