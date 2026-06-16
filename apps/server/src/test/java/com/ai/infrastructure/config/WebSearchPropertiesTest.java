package com.ai.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for WebSearchProperties configuration binding.
 */
@DisplayName("WebSearchProperties")
class WebSearchPropertiesTest {

    @Test
    @DisplayName("should have default values")
    void shouldHaveDefaultValues() {
        WebSearchProperties props = new WebSearchProperties();
        assertThat(props.getBaseUrl()).isEqualTo("https://html.duckduckgo.com/html/");
        assertThat(props.getUserAgent()).isEqualTo("Mozilla/5.0 (compatible; ai-infra/0.1)");
        assertThat(props.getDefaultMaxResults()).isEqualTo(5);
        assertThat(props.getDefaultRegion()).isEqualTo("us-en");
        assertThat(props.getTimeoutMs()).isEqualTo(5000);
    }

    @Test
    @DisplayName("should set and get base-url")
    void shouldSetAndGetBaseUrl() {
        WebSearchProperties props = new WebSearchProperties();
        props.setBaseUrl("https://custom.duckduckgo.com/");
        assertThat(props.getBaseUrl()).isEqualTo("https://custom.duckduckgo.com/");
    }

    @Test
    @DisplayName("should set and get user-agent")
    void shouldSetAndGetUserAgent() {
        WebSearchProperties props = new WebSearchProperties();
        props.setUserAgent("CustomAgent/1.0");
        assertThat(props.getUserAgent()).isEqualTo("CustomAgent/1.0");
    }

    @Test
    @DisplayName("should set and get default-max-results")
    void shouldSetAndGetDefaultMaxResults() {
        WebSearchProperties props = new WebSearchProperties();
        props.setDefaultMaxResults(10);
        assertThat(props.getDefaultMaxResults()).isEqualTo(10);
    }

    @Test
    @DisplayName("should set and get default-region")
    void shouldSetAndGetDefaultRegion() {
        WebSearchProperties props = new WebSearchProperties();
        props.setDefaultRegion("cn-zh");
        assertThat(props.getDefaultRegion()).isEqualTo("cn-zh");
    }

    @Test
    @DisplayName("should set and get timeout-ms")
    void shouldSetAndGetTimeoutMs() {
        WebSearchProperties props = new WebSearchProperties();
        props.setTimeoutMs(3000);
        assertThat(props.getTimeoutMs()).isEqualTo(3000);
    }
}
