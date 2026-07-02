package com.ai.common.domain.port.out;

/**
 * Port interface for web search capabilities.
 * Implemented by adapters to provide real-time web search.
 */
public interface WebSearchTool {

    /**
     * Search the web for real-time information.
     *
     * @param query the search query
     * @return formatted search results with sources
     */
    String searchWeb(String query);
}
