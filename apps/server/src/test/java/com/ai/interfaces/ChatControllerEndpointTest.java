package com.ai.interfaces;

import com.ai.service.AiChatService;
import com.ai.domain.model.AiServiceException;
import com.ai.interfaces.controller.AiController;
import com.ai.interfaces.controller.GlobalExceptionHandler;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("AiController Endpoint Tests")
class ChatControllerEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiChatService chatService;

    @Nested
    @DisplayName("POST /api/chat endpoint")
    class ChatEndpointTests {

        @Test
        @DisplayName("should return chat response for valid request with message")
        void shouldReturnChatResponseForValidRequestWithMessage() throws Exception {
            when(chatService.processChatMessage(anyString()))
                    .thenReturn("Hello! How can I help you?");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", "Hello"
            ));

            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value("Hello! How can I help you?"));

            verify(chatService).processChatMessage("Hello");
        }

        @Test
        @DisplayName("should return chat response for valid request with session ID")
        void shouldReturnChatResponseForValidRequestWithSessionId() throws Exception {
            String sessionId = "session-123";
            when(chatService.processChatMessage(sessionId, "Hello"))
                    .thenReturn("Response with context");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", "Hello",
                    "sessionId", sessionId
            ));

            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value("Response with context"));

            verify(chatService).processChatMessage(sessionId, "Hello");
        }

        @Test
        @DisplayName("should return 400 for null message")
        void shouldReturn400ForNullMessage() throws Exception {
            String requestBody = "{\"message\": null}";

            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.response").value("Please provide a message."));
        }

        @Test
        @DisplayName("should return 400 for blank message")
        void shouldReturn400ForBlankMessage() throws Exception {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", "   "
            ));

            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.response").value("Please provide a message."));
        }

        @Test
        @DisplayName("should return 500 when service throws AiServiceException")
        void shouldReturn500WhenServiceThrowsAiServiceException() throws Exception {
            when(chatService.processChatMessage(anyString()))
                    .thenThrow(new AiServiceException("AI service unavailable"));

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", "Hello"
            ));

            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("AI_SERVICE_ERROR"));
        }
    }

    @Nested
    @DisplayName("POST /api/chat/simple endpoint")
    class SimpleChatEndpointTests {

        @Test
        @DisplayName("should return response for valid simple request")
        void shouldReturnResponseForValidSimpleRequest() throws Exception {
            when(chatService.processChatMessage("Simple message"))
                    .thenReturn("Simple response");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", "Simple message"
            ));

            mockMvc.perform(post("/api/chat/simple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value("Simple response"));
        }

        @Test
        @DisplayName("should return 400 for missing message in simple request")
        void shouldReturn400ForMissingMessageInSimpleRequest() throws Exception {
            String requestBody = objectMapper.writeValueAsString(Map.of());

            mockMvc.perform(post("/api/chat/simple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.response").value("Please provide a message."));
        }
    }

    @Nested
    @DisplayName("GET /api/health endpoint")
    class HealthEndpointTests {

        @Test
        @DisplayName("should return 200 OK with status UP")
        void shouldReturn200OkWithStatusUp() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }
}
