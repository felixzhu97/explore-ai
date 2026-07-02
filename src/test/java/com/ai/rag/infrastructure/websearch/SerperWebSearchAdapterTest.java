package com.ai.rag.infrastructure.websearch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SerperWebSearchAdapter Tests")
class SerperWebSearchAdapterTest {

    @Test
    @DisplayName("should return error message when query is blank")
    void shouldReturnErrorWhenQueryBlank() {
        SerperWebSearchAdapter adapter = new SerperWebSearchAdapter("fake-key");
        String result = adapter.searchWeb("  ");
        assertThat(result).isEqualTo("Please provide a valid search query.");
    }

    @Test
    @DisplayName("should return error message when query is null")
    void shouldReturnErrorWhenQueryNull() {
        SerperWebSearchAdapter adapter = new SerperWebSearchAdapter("fake-key");
        String result = adapter.searchWeb(null);
        assertThat(result).isEqualTo("Please provide a valid search query.");
    }

    @Test
    @DisplayName("should return error message when API key is empty")
    void shouldReturnErrorWhenApiKeyEmpty() {
        SerperWebSearchAdapter adapter = new SerperWebSearchAdapter("");
        String result = adapter.searchWeb("test query");
        assertThat(result).contains("not available");
    }

    @Test
    @DisplayName("should return error message when API key is not configured")
    void shouldReturnErrorWhenApiKeyNotConfigured() {
        SerperWebSearchAdapter adapter = new SerperWebSearchAdapter("  ");
        String result = adapter.searchWeb("test query");
        assertThat(result).contains("not available");
    }
}
