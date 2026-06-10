package com.ai.integration.controller;

import com.ai.application.service.ChatApplicationService;
import com.ai.interfaces.controller.ChatController;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatController Integration Tests
 * 
 * Tests using @WebMvcTest and MockMvc for controller layer:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 */
@WebMvcTest(controllers = ChatController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ChatController Integration Tests")
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatApplicationService chatService;

    @Nested
    @DisplayName("shouldReturnOk_WhenValidMessage")
    class ValidMessageTests {

        @Test
        @DisplayName("should return OK with response when valid message is sent")
        void shouldReturnOkWithResponseWhenValidMessageSent() throws Exception {
            // Arrange
            ChatRequest request = new ChatRequest("Hello, AI!", null);
            String expectedResponse = "Hello, human! How can I help you?";
            when(chatService.processChatMessage(anyString())).thenReturn(expectedResponse);

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value(expectedResponse));
        }

        @Test
        @DisplayName("should return OK with empty response when AI returns empty")
        void shouldReturnOkWithEmptyResponseWhenAIReturnsEmpty() throws Exception {
            // Arrange
            ChatRequest request = new ChatRequest("Hello", null);
            when(chatService.processChatMessage(anyString())).thenReturn("");

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").isEmpty());
        }

        @Test
        @DisplayName("should handle message with special characters")
        void shouldHandleMessageWithSpecialCharacters() throws Exception {
            // Arrange
            ChatRequest request = new ChatRequest("Hello! ¿Cómo estás? 你好世界!", null);
            when(chatService.processChatMessage(anyString())).thenReturn("Response");

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value("Response"));
        }

        @Test
        @DisplayName("should handle unicode message")
        void shouldHandleUnicodeMessage() throws Exception {
            // Arrange
            ChatRequest request = new ChatRequest("你好世界 🚀", null);
            when(chatService.processChatMessage(anyString())).thenReturn("Response");

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value("Response"));
        }

        @Test
        @DisplayName("should use specified session when sessionId is provided")
        void shouldUseSpecifiedSessionWhenSessionIdIsProvided() throws Exception {
            // Arrange
            ChatRequest request = new ChatRequest("Hello", "session-123");
            String expectedResponse = "Response in session 123";
            when(chatService.processChatMessage(anyString(), anyString())).thenReturn(expectedResponse);

            // Act & Assert
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
            // Arrange
            ChatRequest request = new ChatRequest(null, null);

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.response").value("Please provide a message."));
        }

        @Test
        @DisplayName("should return bad request when message is empty")
        void shouldReturnBadRequestWhenMessageIsEmpty() throws Exception {
            // Arrange
            ChatRequest request = new ChatRequest("", null);

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.response").value("Please provide a message."));
        }

        @Test
        @DisplayName("should return bad request when message is blank")
        void shouldReturnBadRequestWhenMessageIsBlank() throws Exception {
            // Arrange
            ChatRequest request = new ChatRequest("   ", null);

            // Act & Assert
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
            // Act & Assert
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("should return proper content type for health endpoint")
        void shouldReturnProperContentTypeForHealthEndpoint() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("should return correct JSON structure for health")
        void shouldReturnCorrectJsonStructureForHealth() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isMap())
                    .andExpect(jsonPath("$.status").exists());
        }
    }

    @Nested
    @DisplayName("shouldHandleAiServiceException")
    class AiServiceExceptionTests {

        @Test
        @DisplayName("should return 500 error when AI service throws exception")
        void shouldReturn500ErrorWhenAIServiceThrowsException() throws Exception {
            // Arrange
            ChatRequest request = new ChatRequest("Hello", null);
            when(chatService.processChatMessage(anyString()))
                    .thenThrow(new RuntimeException("AI service error"));

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.type").value("error"));
        }

        @Test
        @DisplayName("should return INTERNAL_ERROR when AI service fails")
        void shouldReturnInternalErrorWhenAIServiceFails() throws Exception {
            // Arrange
            ChatRequest request = new ChatRequest("Hello", null);
            when(chatService.processChatMessage(anyString()))
                    .thenThrow(new RuntimeException("Connection timeout"));

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
        }

        @Test
        @DisplayName("should include error type in response")
        void shouldIncludeErrorTypeInResponse() throws Exception {
            // Arrange
            ChatRequest request = new ChatRequest("Hello", null);
            when(chatService.processChatMessage(anyString()))
                    .thenThrow(new RuntimeException("Error"));

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
        }

        @Test
        @DisplayName("should handle different exception types")
        void shouldHandleDifferentExceptionTypes() throws Exception {
            // Arrange
            ChatRequest request = new ChatRequest("Hello", null);
            when(chatService.processChatMessage(anyString()))
                    .thenThrow(new IllegalStateException("Invalid state"));

            // Act & Assert
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
        }
    }

    @Nested
    @DisplayName("Simple Chat Endpoint")
    class SimpleChatEndpointTests {

        @Test
        @DisplayName("should return OK with response for simple chat")
        void shouldReturnOkWithResponseForSimpleChat() throws Exception {
            // Arrange
            String requestBody = objectMapper.writeValueAsString(
                    java.util.Map.of("message", "Hello, AI!")
            );
            when(chatService.processChatMessage("Hello, AI!")).thenReturn("Hello!");

            // Act & Assert
            mockMvc.perform(post("/api/chat/simple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.response").value("Hello!"));
        }

        @Test
        @DisplayName("should return bad request for empty message in simple chat")
        void shouldReturnBadRequestForEmptyMessageInSimpleChat() throws Exception {
            // Arrange
            String requestBody = objectMapper.writeValueAsString(
                    java.util.Map.of("message", "")
            );

            // Act & Assert
            mockMvc.perform(post("/api/chat/simple")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.response").value("Please provide a message."));
        }
    }
}
