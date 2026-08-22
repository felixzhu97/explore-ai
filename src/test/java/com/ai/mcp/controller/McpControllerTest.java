package com.ai.mcp.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.testsupport.SliceWebMvcTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = McpController.class)
@DisplayName("McpController")
class McpControllerTest {

  @Autowired private MockMvcTester mvc;

  @Nested
  @DisplayName("GET /api/mcp/health")
  class Health {

    @Test
    @DisplayName("should return UP status")
    void shouldReturnUpStatus() {
      assertThat(mvc.get().uri("/api/mcp/health"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.status")
          .asString()
          .isEqualTo("UP");

      assertThat(mvc.get().uri("/api/mcp/health"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.server")
          .asString()
          .isEqualTo("explore-ai-mcp-server");

      assertThat(mvc.get().uri("/api/mcp/health"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.protocol")
          .asString()
          .isEqualTo("MCP 1.0");
    }
  }

  @Nested
  @DisplayName("GET /api/mcp/info")
  class Info {

    @Test
    @DisplayName("should return complete MCP server information")
    void shouldReturnCompleteMcpServerInformation() {
      assertThat(mvc.get().uri("/api/mcp/info"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.name")
          .asString()
          .isEqualTo("explore-ai-mcp-server");

      assertThat(mvc.get().uri("/api/mcp/info"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.capabilities.tools")
          .asBoolean()
          .isTrue();

      assertThat(mvc.get().uri("/api/mcp/info"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.availableTools.get_weather")
          .asString()
          .isEqualTo("Get current weather for a city");
    }
  }
}
