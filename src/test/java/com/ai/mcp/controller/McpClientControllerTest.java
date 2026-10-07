package com.ai.mcp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ai.mcp.domain.model.McpServerConnection;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.service.McpService;
import com.ai.testsupport.SliceWebMvcTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = McpClientController.class)
@DisplayName("McpClientController")
class McpClientControllerTest {

  @DynamicPropertySource
  static void enableMcpModule(DynamicPropertyRegistry registry) {
    registry.add("launchdarkly.bootstrap.module-mcp", () -> "true");
  }

  @Autowired private MockMvcTester mvc;

  @MockitoBean private McpService mcpService;

  @Test
  @DisplayName("should return READY status with tool count")
  void shouldReturnReadyStatusWithToolCount() {
    when(mcpService.getTotalToolCount()).thenReturn(5);
    when(mcpService.getConnectedServers()).thenReturn(Map.of());

    assertThat(mvc.get().uri("/api/mcp/client/status"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.registeredTools")
        .convertTo(Integer.class)
        .isEqualTo(5);
  }

  @Test
  @DisplayName("should return list of connected servers")
  void shouldReturnListOfConnectedServers() {
    when(mcpService.getConnectedServers())
        .thenReturn(Map.of("server1", McpServerConnection.createConnectedServer("server1", 3)));

    assertThat(mvc.get().uri("/api/mcp/client/servers"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.length()")
        .convertTo(Integer.class)
        .isEqualTo(1);

    assertThat(mvc.get().uri("/api/mcp/client/servers"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].status")
        .asString()
        .isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("should return list of registered MCP tools")
  void shouldReturnListOfRegisteredMcpTools() {
    when(mcpService.getToolDefinitions())
        .thenReturn(
            List.of(McpToolDefinition.createDefinition("get_weather", "Get current weather")));

    assertThat(mvc.get().uri("/api/mcp/client/tools"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.length()")
        .convertTo(Integer.class)
        .isEqualTo(1);

    assertThat(mvc.get().uri("/api/mcp/client/tools"))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].name")
        .asString()
        .isEqualTo("get_weather");
  }

  @Test
  @DisplayName("should return bad request when question is blank")
  void shouldReturnBadRequestWhenQuestionIsBlank() {
    assertThat(
            mvc.post()
                .uri("/api/mcp/client/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"  \"}"))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.errorCode")
        .asString()
        .isEqualTo("VALIDATION_ERROR");
  }
}
