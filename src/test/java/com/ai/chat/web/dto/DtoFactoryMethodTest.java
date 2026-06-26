package com.ai.chat.web.dto;

import com.ai.chat.domain.model.ChatMessage;
import com.ai.chat.domain.model.ChatSession;
import com.ai.chat.domain.vo.ChatSessionId;
import com.ai.chat.domain.vo.MessageId;
import com.ai.rag.web.dto.DocumentSummaryDto;
import com.ai.rag.web.dto.RagChatRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DTO Factory Method Tests
 *
 * Tests DTO factory methods and record constructors.
 * These are pure unit tests with no Spring context.
 */
@DisplayName("DTO Factory Method Tests")
class DtoFactoryMethodTest {

    @Nested
    @DisplayName("ChatResponse factory methods")
    class ChatResponseFactoryTests {

        @Test
        @DisplayName("should create ChatResponse with only response text")
        void shouldCreateChatResponseWithOnlyResponseText() {
            // Act
            ChatResponse response = ChatResponse.of("Hello, world!");

            // Assert
            assertThat(response.response()).isEqualTo("Hello, world!");
            assertThat(response.sessionId()).isNull();
            assertThat(response.messageId()).isNull();
            assertThat(response.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("should create ChatResponse with all fields")
        void shouldCreateChatResponseWithAllFields() {
            // Arrange
            String sessionId = "session-123";
            String messageId = "msg-456";

            // Act
            ChatResponse response = ChatResponse.of("Response text", sessionId, messageId);

            // Assert
            assertThat(response.response()).isEqualTo("Response text");
            assertThat(response.sessionId()).isEqualTo(sessionId);
            assertThat(response.messageId()).isEqualTo(messageId);
            assertThat(response.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("should create ChatResponse from ChatMessage")
        void shouldCreateChatResponseFromChatMessage() {
            // Arrange
            String messageText = "Test message";
            String sessionIdStr = "session-abc";
            MessageId messageId = MessageId.generate();
            Instant timestamp = Instant.parse("2024-01-15T10:30:00Z");
            ChatMessage message = ChatMessage.of(messageId, messageText, "user", timestamp);

            // Act
            ChatResponse response = ChatResponse.fromMessage(message, sessionIdStr);

            // Assert
            assertThat(response.response()).isEqualTo(messageText);
            assertThat(response.sessionId()).isEqualTo(sessionIdStr);
            assertThat(response.messageId()).isEqualTo(messageId.toString());
            assertThat(response.timestamp()).isEqualTo(timestamp);
        }

        @Test
        @DisplayName("should handle empty response text")
        void shouldHandleEmptyResponseText() {
            // Act
            ChatResponse response = ChatResponse.of("");

            // Assert
            assertThat(response.response()).isEmpty();
            assertThat(response.timestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("RagChatRequest validation and normalization")
    class RagChatRequestNormalizationTests {

        @Test
        @DisplayName("should normalize null topK to default value of 5")
        void shouldNormalizeNullTopKToDefaultValueOf5() {
            // Act
            RagChatRequest request = new RagChatRequest(null, null, null, null, null);

            // Assert
            assertThat(request.topK()).isEqualTo(5);
        }

        @Test
        @DisplayName("should normalize null temperature to default value of 0.7")
        void shouldNormalizeNullTemperatureToDefaultValueOf07() {
            // Act
            RagChatRequest request = new RagChatRequest(null, null, null, null, null);

            // Assert
            assertThat(request.temperature()).isEqualTo(0.7);
        }

        @Test
        @DisplayName("should keep valid topK unchanged")
        void shouldKeepValidTopKUnchanged() {
            // Act
            RagChatRequest request = new RagChatRequest("Question", "session", 10, 0.5, null);

            // Assert
            assertThat(request.topK()).isEqualTo(10);
            assertThat(request.temperature()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("should keep valid temperature unchanged")
        void shouldKeepValidTemperatureUnchanged() {
            // Act
            RagChatRequest request = new RagChatRequest("Question", null, 3, 0.9, null);

            // Assert
            assertThat(request.temperature()).isEqualTo(0.9);
        }

        @Test
        @DisplayName("should handle boundary topK values")
        void shouldHandleBoundaryTopKValues() {
            // Act
            RagChatRequest request1 = new RagChatRequest("Q", null, 1, null, null);
            RagChatRequest request2 = new RagChatRequest("Q", null, 100, null, null);

            // Assert
            assertThat(request1.topK()).isEqualTo(1);
            assertThat(request2.topK()).isEqualTo(100);
        }

        @Test
        @DisplayName("should handle zero topK")
        void shouldHandleZeroTopK() {
            // Act
            RagChatRequest request = new RagChatRequest("Question", null, 0, null, null);

            // Assert - note: current implementation does NOT normalize 0 to 1
            // This test documents the current behavior
            assertThat(request.topK()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle negative topK")
        void shouldHandleNegativeTopK() {
            // Act
            RagChatRequest request = new RagChatRequest("Question", null, -5, null, null);

            // Assert - note: current implementation does NOT normalize negative values
            assertThat(request.topK()).isEqualTo(-5);
        }
    }

    @Nested
    @DisplayName("SessionInfo factory methods")
    class SessionInfoFactoryTests {

        @Test
        @DisplayName("should create SessionInfo from ChatSession")
        void shouldCreateSessionInfoFromChatSession() {
            // Arrange
            ChatSession session = createTestSession("session-456", "My Chat");

            // Act
            SessionInfo info = SessionInfo.from(session);

            // Assert
            assertThat(info.sessionId()).isEqualTo("session-456");
            assertThat(info.title()).isEqualTo("My Chat");
            assertThat(info.messageCount()).isEqualTo(0);
            assertThat(info.createdAt()).isNotNull();
            assertThat(info.lastActivityAt()).isNotNull();
        }

        @Test
        @DisplayName("should create SessionInfo from session with messages")
        void shouldCreateSessionInfoFromSessionWithMessages() {
            // Arrange
            ChatSession session = createTestSessionWithMessages("session-msgs", "Chat with Messages",
                    List.of(
                            ChatMessage.of(MessageId.generate(), "Q1", "user", Instant.now()),
                            ChatMessage.of(MessageId.generate(), "A1", "assistant", Instant.now())
                    ));

            // Act
            SessionInfo info = SessionInfo.from(session);

            // Assert
            assertThat(info.sessionId()).isEqualTo("session-msgs");
            assertThat(info.messageCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should include timestamps in SessionInfo")
        void shouldIncludeTimestampsInSessionInfo() {
            // Arrange
            Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
            ChatSession session = createTestSessionWithTimestamp("timestamp-session", "Timestamp Test", createdAt);

            // Act
            SessionInfo info = SessionInfo.from(session);

            // Assert
            assertThat(info.createdAt()).isEqualTo(createdAt);
            assertThat(info.lastActivityAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("DocumentSummaryDto record accessors")
    class DocumentSummaryDtoAccessorTests {

        @Test
        @DisplayName("should have correct accessor values")
        void shouldHaveCorrectAccessorValues() {
            // Arrange
            UUID id = UUID.randomUUID();
            Instant createdAt = Instant.parse("2024-01-15T10:30:00Z");

            // Act
            DocumentSummaryDto dto = new DocumentSummaryDto(id, "Test Document", "READY", createdAt, 10);

            // Assert
            assertThat(dto.id()).isEqualTo(id);
            assertThat(dto.title()).isEqualTo("Test Document");
            assertThat(dto.status()).isEqualTo("READY");
            assertThat(dto.createdAt()).isEqualTo(createdAt);
            assertThat(dto.chunkCount()).isEqualTo(10);
        }

        @Test
        @DisplayName("should handle zero chunk count")
        void shouldHandleZeroChunkCount() {
            // Arrange
            DocumentSummaryDto dto = new DocumentSummaryDto(UUID.randomUUID(), "Empty Doc", "PROCESSING", Instant.now(), 0);

            // Assert
            assertThat(dto.chunkCount()).isZero();
        }

        @Test
        @DisplayName("should handle different status values")
        void shouldHandleDifferentStatusValues() {
            // Test READY status
            DocumentSummaryDto readyDto = new DocumentSummaryDto(UUID.randomUUID(), "Ready Doc", "READY", Instant.now(), 5);
            assertThat(readyDto.status()).isEqualTo("READY");

            // Test PROCESSING status
            DocumentSummaryDto processingDto = new DocumentSummaryDto(UUID.randomUUID(), "Processing Doc", "PROCESSING", Instant.now(), 3);
            assertThat(processingDto.status()).isEqualTo("PROCESSING");

            // Test FAILED status
            DocumentSummaryDto failedDto = new DocumentSummaryDto(UUID.randomUUID(), "Failed Doc", "FAILED", Instant.now(), 0);
            assertThat(failedDto.status()).isEqualTo("FAILED");
        }
    }

    @Nested
    @DisplayName("ChatRequest record features")
    class ChatRequestRecordTests {

        @Test
        @DisplayName("should trim message on construction")
        void shouldTrimMessageOnConstruction() {
            // Act
            ChatRequest request = new ChatRequest("  Hello world  ", null);

            // Assert
            assertThat(request.message()).isEqualTo("Hello world");
        }

        @Test
        @DisplayName("should handle null message without trimming")
        void shouldHandleNullMessageWithoutTrimming() {
            // Act
            ChatRequest request = new ChatRequest(null, null);

            // Assert
            assertThat(request.message()).isNull();
        }

        @Test
        @DisplayName("should create ChatRequest with session ID")
        void shouldCreateChatRequestWithSessionId() {
            // Act
            ChatRequest request = ChatRequest.of("Hello", "session-123");

            // Assert
            assertThat(request.message()).isEqualTo("Hello");
            assertThat(request.sessionId()).isEqualTo("session-123");
        }

        @Test
        @DisplayName("should create ChatRequest without session ID")
        void shouldCreateChatRequestWithoutSessionId() {
            // Act
            ChatRequest request = ChatRequest.of("Hello");

            // Assert
            assertThat(request.message()).isEqualTo("Hello");
            assertThat(request.sessionId()).isNull();
        }

        @Test
        @DisplayName("should preserve empty message")
        void shouldPreserveEmptyMessage() {
            // Act
            ChatRequest request = new ChatRequest("", null);

            // Assert
            assertThat(request.message()).isEmpty();
        }
    }

    // Helper methods

    private ChatSession createTestSession(String id, String title) {
        return createTestSessionWithMessages(id, title, List.of());
    }

    private ChatSession createTestSessionWithMessages(String id, String title, List<ChatMessage> messages) {
        ChatSession session = ChatSession.create(title);
        setSessionId(session, id);
        addMessagesToSession(session, messages);
        return session;
    }

    private ChatSession createTestSessionWithTimestamp(String id, String title, Instant createdAt) {
        ChatSession session = ChatSession.of(
                ChatSessionId.of(id),
                title,
                createdAt
        );
        return session;
    }

    private void setSessionId(ChatSession session, String id) {
        try {
            var idField = ChatSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(session, ChatSessionId.of(id));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addMessagesToSession(ChatSession session, List<ChatMessage> messages) {
        try {
            var messagesField = ChatSession.class.getDeclaredField("messages");
            messagesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var list = (java.util.List<ChatMessage>) messagesField.get(session);
            list.addAll(messages);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
