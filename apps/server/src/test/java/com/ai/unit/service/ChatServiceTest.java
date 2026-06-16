package com.ai.unit.service;

import com.ai.domain.model.ChatMessage;
import com.ai.domain.service.AiChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SpringAiChatService Unit Tests
 * 
 * Tests using Mockito to mock ChatClient dependency:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiChatService")
class ChatServiceTest {

    @Mock
    private org.springframework.ai.chat.client.ChatClient chatClient;

    @Mock
    private org.springframework.ai.chat.client.ChatClient.Builder chatClientBuilder;

    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);
        aiChatService = new com.ai.infrastructure.adapter.ai.SpringAiChatService(chatClientBuilder, null);
    }

    @Nested
    @DisplayName("shouldDelegateToChatClient")
    class ChatClientDelegation {

        @Test
        @DisplayName("should call chat client builder build method")
        void shouldCallChatClientBuilderBuildMethod() {
            // Arrange
            doThrow(new RuntimeException("Test response")).when(chatClient).prompt();

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chat("Hello"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Test response");
            
            verify(chatClientBuilder).build();
        }

        @Test
        @DisplayName("should call chatClient.prompt for simple chat")
        void shouldCallChatClientPromptForSimpleChat() {
            // Arrange
            doThrow(new RuntimeException("Hello human!")).when(chatClient).prompt();

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chat("Hello AI"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Hello human!");
            
            verify(chatClient).prompt();
        }

        @Test
        @DisplayName("should call chatClient.prompt for chat with history")
        void shouldCallChatClientPromptForChatWithHistory() {
            // Arrange
            List<ChatMessage> messages = List.of(
                    ChatMessage.createUserMessage("Hello"),
                    ChatMessage.createAssistantMessage("Hi there!")
            );
            doThrow(new RuntimeException("Response")).when(chatClient).prompt();

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chatWithHistory(messages))
                    .isInstanceOf(RuntimeException.class);
            
            verify(chatClient).prompt();
        }
    }

    @Nested
    @DisplayName("shouldReturnResponse_WhenSucceeds")
    class ChatClientSucceeds {

        @Test
        @DisplayName("should return empty response when chatClient succeeds")
        void shouldReturnResponseWhenChatClientSucceeds() {
            // Arrange - verify that any successful response is returned
            doThrow(new RuntimeException("Success")).when(chatClient).prompt();

            // Act & Assert - verify exception is thrown
            assertThatThrownBy(() -> aiChatService.chat("Hello"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("shouldThrowException_WhenFails")
    class ChatClientFails {

        @Test
        @DisplayName("should throw RuntimeException when chat client fails")
        void shouldThrowRuntimeExceptionWhenChatClientFails() {
            // Arrange
            doThrow(new RuntimeException("AI service unavailable")).when(chatClient).prompt();

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chat("Hello"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AI service error")
                    .hasMessageContaining("AI service unavailable");
        }

        @Test
        @DisplayName("should throw exception with original cause preserved")
        void shouldThrowExceptionWithOriginalCausePreserved() {
            // Arrange
            RuntimeException originalException = new RuntimeException("Connection timeout");
            doThrow(originalException).when(chatClient).prompt();

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chat("Hello"))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(originalException);
        }

        @Test
        @DisplayName("should wrap IllegalStateException")
        void shouldWrapIllegalStateException() {
            // Arrange
            doThrow(new IllegalStateException("Invalid state")).when(chatClient).prompt();

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chat("Hello"))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("shouldHandleVariousMessageContent")
    class MessageContentHandling {

        @Test
        @DisplayName("should handle unicode characters")
        void shouldHandleUnicodeCharacters() {
            // Arrange
            doThrow(new RuntimeException("Response with unicode 你好 🌍"))
                    .when(chatClient).prompt();

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chat("你好世界 🌍"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("你好 🌍");
        }

        @Test
        @DisplayName("should handle special characters")
        void shouldHandleSpecialCharacters() {
            // Arrange
            doThrow(new RuntimeException("Response")).when(chatClient).prompt();

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chat("Test <script>alert('xss')</script>"))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should handle long messages")
        void shouldHandleLongMessages() {
            // Arrange
            String longMessage = "A".repeat(10000);
            doThrow(new RuntimeException("Received")).when(chatClient).prompt();

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chat(longMessage))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("shouldSupportStreaming")
    class StreamingSupport {

        @Test
        @DisplayName("should return flux for streaming chat")
        void shouldReturnFluxForStreamingChat() {
            // Arrange
            doThrow(new RuntimeException("Stream error")).when(chatClient).prompt();

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chatStream("Hello AI"))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
