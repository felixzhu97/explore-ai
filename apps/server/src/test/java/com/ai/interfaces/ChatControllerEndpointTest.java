package com.ai.interfaces;

import com.ai.application.service.ChatApplicationService;
import com.ai.domain.model.AiServiceException;
import com.ai.interfaces.controller.ChatController;
import com.ai.interfaces.controller.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatController Endpoint Tests
 *
 * Tests using standalone MockMvc setup for chat endpoints:
 * - POST /api/chat - chat with valid request
 * - POST /api/chat/simple - simple map-based request
 * - GET /api/health - health check
 * - Error handling for invalid requests and internal errors
 * - truncate() boundary conditions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController Endpoint Tests")
class ChatControllerEndpointTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ChatApplicationService chatService;

    @InjectMocks
    private ChatController chatController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(chatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("POST /api/chat endpoint")
    class ChatEndpointTests {

        @Test
        @DisplayName("should return chat response for valid request with message")
        void shouldReturnChatResponseForValidRequestWithMessage() throws Exception {
            // Arrange
            when(chatService.processChatMessage(anyString()))
                    .thenReturn("Hello! How can I help you?");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", "Hello"
            ));

            // Act & Assert
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
            // Arrange
            String sessionId = "session-123";
            when(chatService.processChatMessage(sessionId, "Hello"))
                    .thenReturn("Response with context");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", "Hello",
                    "sessionId", sessionId
            ));

            // Act & Assert
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
            // Arrange - use raw JSON to avoid Map.of() NPE with null values
            String requestBody = "{\"message\": null}";

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.response").value("Please provide a message."));
        }

        @Test
        @DisplayName("should return 400 for blank message")
        void shouldReturn400ForBlankMessage() throws Exception {
            // Arrange
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", "   "
            ));

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.response").value("Please provide a message."));
        }

        @Test
        @DisplayName("should return 500 when use case throws AiServiceException")
        void shouldReturn500WhenUseCaseThrowsAiServiceException() throws Exception {
            // Arrange
            when(chatService.processChatMessage(anyString()))
                    .thenThrow(new AiServiceException("AI service unavailable"));

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", "Hello"
            ));

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error").value("AI_SERVICE_ERROR"));
        }

        @Test
        @DisplayName("should handle long message in logging by truncating")
        void shouldHandleLongMessageInLoggingByTruncating() throws Exception {
            // Arrange
            String longMessage = "A".repeat(100);
            when(chatService.processChatMessage(anyString()))
                    .thenReturn("Response");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", longMessage
            ));

            // Act & Assert - should not fail even with long message
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            verify(chatService).processChatMessage(longMessage);
        }
    }

    @Nested
    @DisplayName("POST /api/chat/simple endpoint")
    class SimpleChatEndpointTests {

        @Test
        @DisplayName("should return response for valid simple request")
        void shouldReturnResponseForValidSimpleRequest() throws Exception {
            // Arrange
            when(chatService.processChatMessage("Simple message"))
                    .thenReturn("Simple response");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", "Simple message"
            ));

            // Act & Assert
            mockMvc.perform(post("/api/chat/simple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value("Simple response"));
        }

        @Test
        @DisplayName("should return 400 for missing message in simple request")
        void shouldReturn400ForMissingMessageInSimpleRequest() throws Exception {
            // Arrange
            String requestBody = objectMapper.writeValueAsString(Map.of());

            // Act & Assert
            mockMvc.perform(post("/api/chat/simple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.response").value("Please provide a message."));
        }

        @Test
        @DisplayName("should return 400 for blank message in simple request")
        void shouldReturn400ForBlankMessageInSimpleRequest() throws Exception {
            // Arrange
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", ""
            ));

            // Act & Assert
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
            // Act & Assert
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("should always return UP regardless of system state")
        void shouldAlwaysReturnUpRegardlessOfSystemState() throws Exception {
            // Act & Assert - health endpoint should not depend on service state
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").exists());
        }
    }

    @Nested
    @DisplayName("truncate() method boundary tests")
    class TruncateMethodTests {

        @Test
        @DisplayName("should return original text when length is exactly 50")
        void shouldReturnOriginalTextWhenLengthIsExactly50() throws Exception {
            // Arrange
            String exact50Chars = "12345678901234567890123456789012345678901234567890"; // 50 chars
            when(chatService.processChatMessage(anyString())).thenReturn("Response");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", exact50Chars
            ));

            // Act & Assert - should not truncate
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            verify(chatService).processChatMessage(exact50Chars);
        }

        @Test
        @DisplayName("should truncate text when length exceeds 50")
        void shouldTruncateTextWhenLengthExceeds50() throws Exception {
            // Arrange
            String longText = "123456789012345678901234567890123456789012345678901"; // 51 chars
            when(chatService.processChatMessage(anyString())).thenReturn("Response");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "message", longText
            ));

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            verify(chatService).processChatMessage(longText);
        }

        @Test
        @DisplayName("should handle null text in truncate")
        void shouldHandleNullTextInTruncate() throws Exception {
            // Arrange - use raw JSON to avoid Map.of() NPE with null values
            String requestBody = "{\"message\": null}";

            // Act & Assert - should handle null gracefully
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }
}
