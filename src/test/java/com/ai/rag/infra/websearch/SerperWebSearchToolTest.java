package com.ai.rag.infra.websearch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SerperWebSearchTool")
class SerperWebSearchToolTest {

  @Test
  @DisplayName("should return error message when query is blank")
  void shouldReturnErrorWhenQueryBlank() {
    SerperWebSearchTool adapter = new SerperWebSearchTool("fake-key");
    String result = adapter.searchWeb("  ");
    assertThat(result).isEqualTo("Please provide a valid search query.");
  }

  @Test
  @DisplayName("should return error message when query is null")
  void shouldReturnErrorWhenQueryNull() {
    SerperWebSearchTool adapter = new SerperWebSearchTool("fake-key");
    String result = adapter.searchWeb(null);
    assertThat(result).isEqualTo("Please provide a valid search query.");
  }

  @Test
  @DisplayName("should return error message when API key is empty")
  void shouldReturnErrorWhenApiKeyEmpty() {
    SerperWebSearchTool adapter = new SerperWebSearchTool("");
    String result = adapter.searchWeb("test query");
    assertThat(result).contains("not available");
  }

  @Test
  @DisplayName("should return error message when API key is not configured")
  void shouldReturnErrorWhenApiKeyNotConfigured() {
    SerperWebSearchTool adapter = new SerperWebSearchTool("  ");
    String result = adapter.searchWeb("test query");
    assertThat(result).contains("not available");
  }
}
