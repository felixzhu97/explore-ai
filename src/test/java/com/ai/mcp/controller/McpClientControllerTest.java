package com.ai.mcp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.vo.McpServerConnection;
import com.ai.mcp.service.usecase.McpFacade;
import com.ai.testsupport.SliceWebMvcTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SliceWebMvcTest(controllers = McpClientController.class)
@DisplayName("McpClientController")
class McpClientControllerTest {

  @Autowired private MockMvcTester mvc;

  @MockitoBean private McpFacade mcpFacade;

  @Nested
  @DisplayName("GET /api/mcp/client/status")
  class GetStatus {

    @Test
    @DisplayName("should return READY status with tool count")
    void shouldReturnReadyStatusWithToolCount() {
      when(mcpFacade.getTotalToolCount()).thenReturn(5);
      when(mcpFacade.getConnectedServers()).thenReturn(Map.of());

      assertThat(mvc.get().uri("/api/mcp/client/status"))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.registeredTools")
          .convertTo(Integer.class)
          .isEqualTo(5);
    }
  }

  @Nested
  @DisplayName("GET /api/mcp/client/servers")
  class ListServers {

    @Test
    @DisplayName("should return list of connected servers")
    void shouldReturnListOfConnectedServers() {
      when(mcpFacade.getConnectedServers())
          .thenReturn(Map.of("server1", McpServerConnection.connected("server1", 3)));

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
  }

  @Nested
  @DisplayName("GET /api/mcp/client/tools")
  class ListTools {

    @Test
    @DisplayName("should return list of registered MCP tools")
    void shouldReturnListOfRegisteredMcpTools() {
      when(mcpFacade.getToolDefinitions())
          .thenReturn(List.of(McpToolDefinition.create("get_weather", "Get current weather")));

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
  }

  @Nested
  @DisplayName("POST /api/mcp/client/chat")
  class Chat {

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
          .extractingPath("$.error")
          .asString()
          .isEqualTo("提问内容不能为空");
    }

    @Test
    @DisplayName("should return bad request when question is null")
    void shouldReturnBadRequestWhenQuestionIsNull() {
      assertThat(
              mvc.post()
                  .uri("/api/mcp/client/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("should return internal server error on service exception")
    void shouldReturnInternalServerErrorOnServiceException() {
      when(mcpFacade.chatWithTools("Hello")).thenThrow(new RuntimeException("Service error"));

      assertThat(
              mvc.post()
                  .uri("/api/mcp/client/chat")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"question\":\"Hello\"}"))
          .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
