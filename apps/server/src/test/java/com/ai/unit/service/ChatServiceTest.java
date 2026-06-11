package com.ai.unit.service;

import com.ai.domain.service.AiChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SpringAiChatService Unit Tests
 * 
 * Tests using Mockito to mock ChatModel dependency:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiChatService")
class ChatServiceTest {

    @Mock
    private ChatModel chatModel;

    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        aiChatService = new com.ai.infrastructure.adapter.ai.SpringAiChatService(chatModel);
    }

    @Nested
    @DisplayName("shouldReturnResponse_WhenChatModelSucceeds")
    class ChatModelSucceeds {

        @Test
        @DisplayName("should return AI response when chat model succeeds")
        void shouldReturnAIResponseWhenChatModelSucceeds() {
            // Arrange
            String userMessage = "Hello, AI!";
            String expectedResponse = "Hello, human! How can I help you?";
            when(chatModel.call(any(Prompt.class)))
                    .thenAnswer(invocation -> createMockChatResponse(expectedResponse));

            // Act
            String actualResponse = aiChatService.chat(userMessage);

            // Assert
            assertThat(actualResponse).isEqualTo(expectedResponse);
            verify(chatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("should return response when chat model returns long text")
        void shouldReturnResponseWhenChatModelReturnsLongText() {
            // Arrange
            String userMessage = "Tell me a story";
            String longResponse = "Once upon a time in a land far, far away...".repeat(100);
            when(chatModel.call(any(Prompt.class)))
                    .thenAnswer(invocation -> createMockChatResponse(longResponse));

            // Act
            String actualResponse = aiChatService.chat(userMessage);

            // Assert
            assertThat(actualResponse).isEqualTo(longResponse);
        }

        @Test
        @DisplayName("should return empty string when AI returns null")
        void shouldReturnEmptyStringWhenAIReturnsNull() {
            // Arrange
            String userMessage = "Hello";
            when(chatModel.call(any(Prompt.class)))
                    .thenAnswer(invocation -> createMockChatResponse(null));

            // Act
            String actualResponse = aiChatService.chat(userMessage);

            // Assert
            assertThat(actualResponse).isEmpty();
        }
    }

    @Nested
    @DisplayName("shouldThrowException_WhenChatModelFails")
    class ChatModelFails {

        @Test
        @DisplayName("should throw RuntimeException when chat model throws exception")
        void shouldThrowRuntimeExceptionWhenChatModelThrowsException() {
            // Arrange
            String userMessage = "Hello";
            when(chatModel.call(any(Prompt.class)))
                    .thenThrow(new RuntimeException("AI service unavailable"));

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chat(userMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AI service error:")
                    .hasMessageContaining("AI service unavailable");
        }

        @Test
        @DisplayName("should throw exception with original cause preserved")
        void shouldThrowExceptionWithOriginalCausePreserved() {
            // Arrange
            String userMessage = "Hello";
            RuntimeException originalException = new RuntimeException("Connection timeout");
            when(chatModel.call(any(Prompt.class))).thenThrow(originalException);

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chat(userMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(originalException);
        }

        @Test
        @DisplayName("should wrap different exception types")
        void shouldWrapDifferentExceptionTypes() {
            // Arrange
            String userMessage = "Hello";
            when(chatModel.call(any(Prompt.class)))
                    .thenThrow(new IllegalStateException("Invalid state"));

            // Act & Assert
            assertThatThrownBy(() -> aiChatService.chat(userMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AI service error:")
                    .hasCauseInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("shouldPassCorrectPromptToModel")
    class PromptConstruction {

        @Test
        @DisplayName("should pass user message as UserMessage in prompt")
        void shouldPassUserMessageAsUserMessageInPrompt() {
            // Arrange
            String userMessage = "What is the weather?";
            when(chatModel.call(any(Prompt.class)))
                    .thenAnswer(invocation -> createMockChatResponse("Sunny"));

            // Act
            aiChatService.chat(userMessage);

            // Assert
            ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
            verify(chatModel).call(promptCaptor.capture());

            Prompt capturedPrompt = promptCaptor.getValue();
            assertThat(capturedPrompt.getInstructions()).isNotEmpty();
        }

        @Test
        @DisplayName("should include user message text in prompt")
        void shouldIncludeUserMessageTextInPrompt() {
            // Arrange
            String userMessage = "Hello world!";
            when(chatModel.call(any(Prompt.class)))
                    .thenAnswer(invocation -> createMockChatResponse("Response"));

            // Act
            aiChatService.chat(userMessage);

            // Assert
            ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
            verify(chatModel).call(promptCaptor.capture());

            Prompt capturedPrompt = promptCaptor.getValue();
            assertThat(capturedPrompt.getInstructions()).isNotEmpty();
            assertThat(capturedPrompt.getInstructions().get(0).getText()).isEqualTo(userMessage);
        }

        @Test
        @DisplayName("should handle special characters in message")
        void shouldHandleSpecialCharactersInMessage() {
            // Arrange
            String userMessage = "Hello! ¿Cómo estás? 你好世界! 🎉";
            when(chatModel.call(any(Prompt.class)))
                    .thenAnswer(invocation -> createMockChatResponse("Response"));

            // Act
            String response = aiChatService.chat(userMessage);

            // Assert
            assertThat(response).isEqualTo("Response");
            verify(chatModel).call(any(Prompt.class));
        }

        @Test
        @DisplayName("should handle unicode characters in message")
        void shouldHandleUnicodeCharactersInMessage() {
            // Arrange
            String userMessage = "Test with émojis 🚀 and unicode 你好";
            when(chatModel.call(any(Prompt.class)))
                    .thenAnswer(invocation -> createMockChatResponse("Response"));

            // Act
            String response = aiChatService.chat(userMessage);

            // Assert
            assertThat(response).isEqualTo("Response");
        }
    }

    @Nested
    @DisplayName("Multiple Calls")
    class MultipleCalls {

        @Test
        @DisplayName("should handle multiple chat calls")
        void shouldHandleMultipleChatCalls() {
            // Arrange - use sequential answers
            when(chatModel.call(any(Prompt.class)))
                    .thenAnswer(invocation -> createMockChatResponse("Response 1"))
                    .thenAnswer(invocation -> createMockChatResponse("Response 2"))
                    .thenAnswer(invocation -> createMockChatResponse("Response 3"));

            // Act
            String response1 = aiChatService.chat("Message 1");
            String response2 = aiChatService.chat("Message 2");
            String response3 = aiChatService.chat("Message 3");

            // Assert
            assertThat(response1).isEqualTo("Response 1");
            assertThat(response2).isEqualTo("Response 2");
            assertThat(response3).isEqualTo("Response 3");
            verify(chatModel, times(3)).call(any(Prompt.class));
        }

        @Test
        @DisplayName("should call chat model for each request")
        void shouldCallChatModelForEachRequest() {
            // Arrange
            when(chatModel.call(any(Prompt.class)))
                    .thenAnswer(invocation -> createMockChatResponse("Response"));

            // Act
            aiChatService.chat("Hello");
            aiChatService.chat("Hello");
            aiChatService.chat("Hello");

            // Assert
            verify(chatModel, times(3)).call(any(Prompt.class));
        }
    }

    private ChatResponse createMockChatResponse(String text) {
        String responseText = text != null ? text : "";
        AssistantMessage assistantMessage = new AssistantMessage(responseText);
        Generation generation = new Generation(assistantMessage);
        return new ChatResponse(List.of(generation));
    }
}
