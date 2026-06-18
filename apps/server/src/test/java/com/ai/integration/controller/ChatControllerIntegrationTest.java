package com.ai.integration.controller;

import com.ai.service.AiChatService;
import com.ai.interfaces.controller.AiController;
import com.ai.interfaces.controller.GlobalExceptionHandler;
import com.ai.interfaces.dto.ChatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AiController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("AiController Integration Tests")
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiChatService chatService;

    @Nested
    @DisplayName("shouldReturnOk_WhenValidMessage")
    class ValidMessageTests {

        @Test
        @DisplayName("should return OK with response when valid message is sent")
        void shouldReturnOkWithResponseWhenValidMessageSent() throws Exception {
            ChatRequest request = new ChatRequest("Hello, AI!", null);
            String expectedResponse = "Hello, human! How can I help you?";
            when(chatService.processChatMessage(anyString())).thenReturn(expectedResponse);

            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value(expectedResponse));
        }

        @Test
        @DisplayName("should use specified session when sessionId is provided")
        void shouldUseSpecifiedSessionWhenSessionIdIsProvided() throws Exception {
            ChatRequest request = new ChatRequest("Hello", "session-123");
            String expectedResponse = "Response in session 123";
            when(chatService.processChatMessage(anyString(), anyString())).thenReturn(expectedResponse);

            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value(expectedResponse));
        }
    }

    @Nested
    @DisplayName("shouldReturnError_WhenEmptyMessage")
    class EmptyMessageTests {

        @Test
        @DisplayName("should return bad request when message is null")
        void shouldReturnBadRequestWhenMessageIsNull() throws Exception {
            ChatRequest request = new ChatRequest(null, null);

            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.response").value("Please provide a message."));
        }

        @Test
        @DisplayName("should return bad request when message is empty")
        void shouldReturnBadRequestWhenMessageIsEmpty() throws Exception {
            ChatRequest request = new ChatRequest("", null);

            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.response").value("Please provide a message."));
        }
    }

    @Nested
    @DisplayName("shouldReturnHealthStatus")
    class HealthEndpointTests {

        @Test
        @DisplayName("should return UP status when health endpoint is called")
        void shouldReturnUpStatusWhenHealthEndpointCalled() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Nested
    @DisplayName("shouldHandleAiServiceException")
    class AiServiceExceptionTests {

        @Test
        @DisplayName("should return 500 error when AI service throws exception")
        void shouldReturn500ErrorWhenAIServiceThrowsException() throws Exception {
            ChatRequest request = new ChatRequest("Hello", null);
            when(chatService.processChatMessage(anyString()))
                    .thenThrow(new RuntimeException("AI service error"));

            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.type").value("error"));
        }
    }
}
