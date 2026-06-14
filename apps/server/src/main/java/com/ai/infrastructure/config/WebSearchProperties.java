package com.ai.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DuckDuckGo web search configuration properties.
 * Binds configuration from application.yml under 'duckduckgo' prefix.
 */
@ConfigurationProperties(prefix = "duckduckgo")
public class WebSearchProperties {

    private String baseUrl = "https://html.duckduckgo.com/html/";
    private String userAgent = "Mozilla/5.0 (compatible; ai-infra/0.1)";
    private int defaultMaxResults = 5;
    private String defaultRegion = "us-en";
    private int timeoutMs = 5000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getDefaultMaxResults() {
        return defaultMaxResults;
    }

    public void setDefaultMaxResults(int defaultMaxResults) {
        this.defaultMaxResults = defaultMaxResults;
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
