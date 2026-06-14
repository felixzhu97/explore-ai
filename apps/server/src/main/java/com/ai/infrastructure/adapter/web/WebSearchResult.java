package com.ai.infrastructure.adapter.web;

/**
 * Record representing a single web search result.
 *
 * @param title the result title
 * @param url the result URL
 * @param snippet the result snippet/description
 * @param rank the result rank (0-based)
 */
public record WebSearchResult(
    String title,
    String url,
    String snippet,
    int rank
) {}
