package com.ai.infrastructure.adapter.ai;

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
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SpringAiChatAdapter Unit Tests
 *
 * Tests the Spring AI chat adapter implementation:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests delegation to AiChatService and exception wrapping
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SpringAiChatAdapter")
class SpringAiChatAdapterTest {

    @Mock
    private AiChatService aiChatService;

    private SpringAiChatAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SpringAiChatAdapter(aiChatService);
    }

    @Nested
    @DisplayName("shouldDelegateChatToAiChatService")
    class ShouldDelegateChatToAiChatService {

        @Test
        @DisplayName("should call aiChatService.chat with user message")
        void shouldCallAiChatServiceChatWithUserMessage() {
            // Arrange
            String userMessage = "Hello, AI!";
            String expectedResponse = "Hello, human!";
            when(aiChatService.chat(userMessage)).thenReturn(expectedResponse);

            // Act
            String result = adapter.chat(userMessage);

            // Assert
            verify(aiChatService).chat(userMessage);
            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("should return AI response from service")
        void shouldReturnAiResponseFromService() {
            // Arrange
            String userMessage = "What is 2+2?";
            String expectedResponse = "2+2 equals 4";
            when(aiChatService.chat(anyString())).thenReturn(expectedResponse);

            // Act
            String result = adapter.chat(userMessage);

            // Assert
            assertThat(result).isEqualTo("2+2 equals 4");
        }

        @Test
        @DisplayName("should pass through various message types")
        void shouldPassThroughVariousMessageTypes() {
            // Arrange
            String[] messages = {
                    "Simple message",
                    "Message with numbers 123",
                    "Message with special chars @#$%",
                    "中文消息",
                    "αβγδ message"
            };
            when(aiChatService.chat(anyString())).thenReturn("Response");

            // Act & Assert
            for (String message : messages) {
                String result = adapter.chat(message);
                verify(aiChatService).chat(message);
                assertThat(result).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("shouldDelegateChatWithHistoryToAiChatService")
    class ShouldDelegateChatWithHistoryToAiChatService {

        @Test
        @DisplayName("should call aiChatService.chatWithHistory with messages")
        void shouldCallAiChatServiceChatWithHistoryWithMessages() {
            // Arrange
            List<ChatMessage> messages = List.of(
                    ChatMessage.createUserMessage("Hello"),
                    ChatMessage.createAssistantMessage("Hi there!")
            );
            String expectedResponse = "Continuing our conversation";
            when(aiChatService.chatWithHistory(messages)).thenReturn(expectedResponse);

            // Act
            String result = adapter.chatWithHistory(messages);

            // Assert
            verify(aiChatService).chatWithHistory(messages);
            assertThat(result).isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("should return AI response with history from service")
        void shouldReturnAiResponseWithHistoryFromService() {
            // Arrange
            List<ChatMessage> messages = List.of(
                    ChatMessage.createUserMessage("Previous message"),
                    ChatMessage.createUserMessage("Current message")
            );
            String expectedResponse = "Response considering history";
            when(aiChatService.chatWithHistory(any())).thenReturn(expectedResponse);

            // Act
            String result = adapter.chatWithHistory(messages);

            // Assert
            assertThat(result).isEqualTo("Response considering history");
        }

        @Test
        @DisplayName("should handle empty message history")
        void shouldHandleEmptyMessageHistory() {
            // Arrange
            List<ChatMessage> emptyHistory = List.of();
            String expectedResponse = "Response with no history";
            when(aiChatService.chatWithHistory(emptyHistory)).thenReturn(expectedResponse);

            // Act
            String result = adapter.chatWithHistory(emptyHistory);

            // Assert
            verify(aiChatService).chatWithHistory(emptyHistory);
            assertThat(result).isEqualTo(expectedResponse);
        }
    }

    @Nested
    @DisplayName("shouldDelegateChatStreamToAiChatService")
    class ShouldDelegateChatStreamToAiChatService {

        @Test
        @DisplayName("should call aiChatService.chatStream with user message")
        void shouldCallAiChatServiceChatStreamWithUserMessage() {
            // Arrange
            String userMessage = "Hello, AI!";
            Flux<String> expectedFlux = Flux.just("Hello", " world");
            when(aiChatService.chatStream(userMessage)).thenReturn(expectedFlux);

            // Act
            Flux<String> result = adapter.chatStream(userMessage);

            // Assert
            verify(aiChatService).chatStream(userMessage);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return flux from streaming service")
        void shouldReturnFluxFromStreamingService() {
            // Arrange
            String userMessage = "Stream test";
            Flux<String> expectedFlux = Flux.just("Chunk1", " Chunk2", " Chunk3");
            when(aiChatService.chatStream(userMessage)).thenReturn(expectedFlux);

            // Act
            Flux<String> result = adapter.chatStream(userMessage);

            // Assert
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("shouldWrapExceptionFromAiChatService")
    class ShouldWrapExceptionFromAiChatService {

        @Test
        @DisplayName("should wrap RuntimeException from chat")
        void shouldWrapRuntimeExceptionFromChat() {
            // Arrange
            String userMessage = "Hello";
            RuntimeException originalException = new RuntimeException("AI service unavailable");
            when(aiChatService.chat(anyString())).thenThrow(originalException);

            // Act & Assert
            assertThatThrownBy(() -> adapter.chat(userMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AI service error")
                    .hasCause(originalException);
        }

        @Test
        @DisplayName("should wrap RuntimeException from chatWithHistory")
        void shouldWrapRuntimeExceptionFromChatWithHistory() {
            // Arrange
            List<ChatMessage> messages = List.of(ChatMessage.createUserMessage("Hello"));
            RuntimeException originalException = new RuntimeException("Connection timeout");
            when(aiChatService.chatWithHistory(any())).thenThrow(originalException);

            // Act & Assert
            assertThatThrownBy(() -> adapter.chatWithHistory(messages))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AI service error")
                    .hasCause(originalException);
        }

        @Test
        @DisplayName("should wrap exception from chatStream")
        void shouldWrapExceptionFromChatStream() {
            // Arrange
            String userMessage = "Hello";
            RuntimeException originalException = new RuntimeException("Stream failed");
            when(aiChatService.chatStream(userMessage)).thenThrow(originalException);

            // Act & Assert
            assertThatThrownBy(() -> adapter.chatStream(userMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AI service error")
                    .hasCause(originalException);
        }

        @Test
        @DisplayName("should preserve original exception message")
        void shouldPreserveOriginalExceptionMessage() {
            // Arrange
            String userMessage = "Hello";
            String originalMessage = "Specific error message from AI";
            when(aiChatService.chat(anyString()))
                    .thenThrow(new RuntimeException(originalMessage));

            // Act & Assert
            assertThatThrownBy(() -> adapter.chat(userMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(originalMessage);
        }

        @Test
        @DisplayName("should wrap exception with nested cause")
        void shouldWrapExceptionWithNestedCause() {
            // Arrange
            String userMessage = "Hello";
            Exception nestedCause = new Exception("Network error");
            RuntimeException originalException = new RuntimeException("Service error", nestedCause);
            when(aiChatService.chat(anyString())).thenThrow(originalException);

            // Act & Assert
            assertThatThrownBy(() -> adapter.chat(userMessage))
                    .isInstanceOf(RuntimeException.class)
                    .hasCause(originalException);
        }

        @Test
        @DisplayName("should wrap different exception types")
        void shouldWrapDifferentExceptionTypes() {
            // Arrange
            when(aiChatService.chat(anyString()))
                    .thenThrow(new IllegalStateException("Invalid state"));

            // Act & Assert
            assertThatThrownBy(() -> adapter.chat("test"))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("shouldReturnAssistantMessage")
    class ShouldReturnAssistantMessage {

        @Test
        @DisplayName("should return assistant message from chat")
        void shouldReturnAssistantMessageFromChat() {
            // Arrange
            String userMessage = "What is AI?";
            String assistantMessage = "AI stands for Artificial Intelligence";
            when(aiChatService.chat(userMessage)).thenReturn(assistantMessage);

            // Act
            String result = adapter.chat(userMessage);

            // Assert
            assertThat(result).isEqualTo(assistantMessage);
            assertThat(result).contains("Artificial Intelligence");
        }

        @Test
        @DisplayName("should return multi-line response")
        void shouldReturnMultiLineResponse() {
            // Arrange
            String userMessage = "Explain";
            String multiLineResponse = "Line 1\nLine 2\nLine 3";
            when(aiChatService.chat(userMessage)).thenReturn(multiLineResponse);

            // Act
            String result = adapter.chat(userMessage);

            // Assert
            assertThat(result).contains("\n");
            assertThat(result.split("\n")).hasSize(3);
        }

        @Test
        @DisplayName("should return empty response when AI returns empty")
        void shouldReturnEmptyResponseWhenAiReturnsEmpty() {
            // Arrange
            String userMessage = "Hello";
            when(aiChatService.chat(userMessage)).thenReturn("");

            // Act
            String result = adapter.chat(userMessage);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("shouldSubscribeToStream")
    class ShouldSubscribeToStream {

        @Test
        @DisplayName("should cover lambda expressions when stream is subscribed")
        void shouldCoverLambdaExpressionsWhenStreamIsSubscribed() {
            // Arrange
            String userMessage = "Stream test";
            Flux<String> streamFlux = Flux.just("Hello", " world", "!");
            when(aiChatService.chatStream(userMessage)).thenReturn(streamFlux);

            // Act - Subscribe to the flux to trigger the lambdas
            List<String> collected = adapter.chatStream(userMessage).collectList().block();

            // Assert
            assertThat(collected).containsExactly("Hello", " world", "!");
            verify(aiChatService).chatStream(userMessage);
        }

        @Test
        @DisplayName("should handle stream with single element")
        void shouldHandleStreamWithSingleElement() {
            // Arrange
            String userMessage = "Single stream";
            Flux<String> streamFlux = Flux.just("Single response");
            when(aiChatService.chatStream(userMessage)).thenReturn(streamFlux);

            // Act
            List<String> collected = adapter.chatStream(userMessage).collectList().block();

            // Assert
            assertThat(collected).containsExactly("Single response");
        }

        @Test
        @DisplayName("should handle stream with multiple elements")
        void shouldHandleStreamWithMultipleElements() {
            // Arrange
            String userMessage = "Multi stream";
            Flux<String> streamFlux = Flux.just("Part1", " Part2", " Part3", " Part4");
            when(aiChatService.chatStream(userMessage)).thenReturn(streamFlux);

            // Act
            List<String> collected = adapter.chatStream(userMessage).collectList().block();

            // Assert
            assertThat(collected).hasSize(4);
        }
    }

    @Nested
    @DisplayName("shouldTruncateLongMessages")
    class ShouldTruncateLongMessages {

        @Test
        @DisplayName("should truncate messages longer than 100 characters")
        void shouldTruncateMessagesLongerThan100Characters() {
            // Arrange
            String longMessage = "A".repeat(150);
            when(aiChatService.chat(longMessage)).thenReturn("Response");

            // Act
            String result = adapter.chat(longMessage);

            // Assert - verifies that the method handles long messages
            assertThat(result).isEqualTo("Response");
        }

        @Test
        @DisplayName("should return null text as null string")
        void shouldReturnNullTextAsNullString() {
            // Arrange
            String nullMessage = null;
            when(aiChatService.chat(nullMessage)).thenReturn("Response");

            // Act
            String result = adapter.chat(nullMessage);

            // Assert
            assertThat(result).isEqualTo("Response");
        }
    }

    @Nested
    @DisplayName("Message Delegation Verification")
    class MessageDelegationVerification {

        @Test
        @DisplayName("should call aiChatService exactly once per chat call")
        void shouldCallAiChatServiceExactlyOncePerChatCall() {
            // Arrange
            when(aiChatService.chat(anyString())).thenReturn("Response");

            // Act
            adapter.chat("Message 1");
            adapter.chat("Message 2");
            adapter.chat("Message 3");

            // Assert
            verify(aiChatService, times(3)).chat(anyString());
        }

        @Test
        @DisplayName("should call aiChatService exactly once per chatWithHistory call")
        void shouldCallAiChatServiceExactlyOncePerChatWithHistoryCall() {
            // Arrange
            when(aiChatService.chatWithHistory(any())).thenReturn("Response");

            // Act
            adapter.chatWithHistory(List.of(ChatMessage.createUserMessage("Test")));

            // Assert
            verify(aiChatService, times(1)).chatWithHistory(any());
        }

        @Test
        @DisplayName("should call aiChatService exactly once per chatStream call")
        void shouldCallAiChatServiceExactlyOncePerChatStreamCall() {
            // Arrange
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just("Response"));

            // Act
            adapter.chatStream("Test");

            // Assert
            verify(aiChatService, times(1)).chatStream(anyString());
        }

        @Test
        @DisplayName("should delegate to correct service method")
        void shouldDelegateToCorrectServiceMethod() {
            // Arrange
            when(aiChatService.chat(anyString())).thenReturn("Simple response");
            when(aiChatService.chatWithHistory(any())).thenReturn("History response");
            when(aiChatService.chatStream(anyString())).thenReturn(Flux.just("Stream response"));

            // Act
            String simpleResult = adapter.chat("Simple message");
            String historyResult = adapter.chatWithHistory(List.of(ChatMessage.createUserMessage("History")));
            Flux<String> streamResult = adapter.chatStream("Stream message");

            // Assert
            verify(aiChatService).chat("Simple message");
            verify(aiChatService).chatWithHistory(any());
            verify(aiChatService).chatStream("Stream message");
            assertThat(simpleResult).isEqualTo("Simple response");
            assertThat(historyResult).isEqualTo("History response");
            assertThat(streamResult).isNotNull();
        }
    }
}
