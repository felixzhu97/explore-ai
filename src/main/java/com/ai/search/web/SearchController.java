package com.ai.search.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Web search controller for RAG enhancement.
 * Uses DuckDuckGo Instant Answer API (free, no key required).
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final RestClient restClient;

    public SearchController() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.duckduckgo.com")
                .build();
    }

    @GetMapping
    public Map<String, Object> search(@RequestParam String q, @RequestParam(defaultValue = "5") int limit) {
        log.info("Web search: {}", q);
        var response = restClient.get()
                .uri("/?q={q}&format=json&no_html=1&skip_disambig=1", q)
                .retrieve()
                .body(Map.class);

        var results = new java.util.ArrayList<Map<String, String>>();

        if (response != null) {
            // Extract Abstract
            String abstractText = (String) response.get("AbstractText");
            String abstractUrl = (String) response.get("AbstractURL");
            if (abstractText != null && !abstractText.isBlank()) {
                results.add(Map.of(
                        "title", (String) response.getOrDefault("Heading", q),
                        "snippet", abstractText,
                        "url", abstractUrl != null ? abstractUrl : ""
                ));
            }

            // Extract RelatedTopics
            @SuppressWarnings("unchecked")
            var topics = (List<Map<String, Object>>) response.get("RelatedTopics");
            if (topics != null) {
                for (var topic : topics) {
                    if (results.size() >= limit) break;
                    String text = (String) topic.get("Text");
                    String url = (String) topic.get("FirstURL");
                    if (text != null) {
                        results.add(Map.of(
                                "title", q,
                                "snippet", text,
                                "url", url != null ? url : ""
                        ));
                    }
                }
            }
        }

        log.info("Search returned {} results for: {}", results.size(), q);
        return Map.of("query", q, "results", results);
    }
}
