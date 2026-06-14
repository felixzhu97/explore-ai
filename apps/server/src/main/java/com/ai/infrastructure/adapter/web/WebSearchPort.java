package com.ai.infrastructure.adapter.web;

import java.util.List;

/**
 * Web search port interface.
 * Abstracts the web search implementation for testability.
 */
public interface WebSearchPort {

    /**
     * Search the web for results.
     *
     * @param query the search query
     * @param maxResults maximum number of results to return
     * @param region the search region code (e.g., "us-en", "cn-zh")
     * @return list of search results
     * @throws WebSearchException if the search fails
     */
    List<WebSearchResult> search(String query, int maxResults, String region) throws WebSearchException;

    /**
     * Exception thrown when web search fails.
     */
    class WebSearchException extends Exception {
        private final int statusCode;
        private final boolean rateLimited;

        public WebSearchException(String message) {
            super(message);
            this.statusCode = -1;
            this.rateLimited = false;
        }

        public WebSearchException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
            this.rateLimited = statusCode == 429;
        }

        public WebSearchException(String message, Throwable cause) {
            super(message, cause);
            this.statusCode = -1;
            this.rateLimited = false;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public boolean isRateLimited() {
            return rateLimited;
        }
    }
}
