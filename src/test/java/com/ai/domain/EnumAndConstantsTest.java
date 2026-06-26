package com.ai.domain;

import com.ai.ai.domain.model.ChatMessageType;
import com.ai.ai.domain.model.ChatSessionStatus;
import com.ai.ai.domain.vo.DomainConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enum and Constants Tests
 *
 * Tests for domain enums and constants.
 */
@DisplayName("Enum and Constants Tests")
class EnumAndConstantsTest {

    @Nested
    @DisplayName("ChatMessageType enum tests")
    class ChatMessageTypeTests {

        @Test
        @DisplayName("should have USER and ASSISTANT values")
        void shouldHaveUserAndAssistantValues() {
            // Assert
            assertThat(ChatMessageType.values())
                    .containsExactly(ChatMessageType.USER, ChatMessageType.ASSISTANT);
        }

        @Test
        @DisplayName("should have exactly 2 values")
        void shouldHaveExactly2Values() {
            // Assert
            assertThat(ChatMessageType.values()).hasSize(2);
        }

        @Test
        @DisplayName("should get value by name")
        void shouldGetValueByName() {
            // Act & Assert
            assertThat(ChatMessageType.valueOf("USER")).isEqualTo(ChatMessageType.USER);
            assertThat(ChatMessageType.valueOf("ASSISTANT")).isEqualTo(ChatMessageType.ASSISTANT);
        }

        @Test
        @DisplayName("USER should have correct name")
        void userShouldHaveCorrectName() {
            // Assert
            assertThat(ChatMessageType.USER.name()).isEqualTo("USER");
        }

        @Test
        @DisplayName("ASSISTANT should have correct name")
        void assistantShouldHaveCorrectName() {
            // Assert
            assertThat(ChatMessageType.ASSISTANT.name()).isEqualTo("ASSISTANT");
        }
    }

    @Nested
    @DisplayName("ChatSessionStatus enum tests")
    class ChatSessionStatusTests {

        @Test
        @DisplayName("should have ACTIVE and CLOSED values")
        void shouldHaveActiveAndClosedValues() {
            // Assert
            assertThat(ChatSessionStatus.values())
                    .containsExactly(ChatSessionStatus.ACTIVE, ChatSessionStatus.CLOSED);
        }

        @Test
        @DisplayName("should have exactly 2 values")
        void shouldHaveExactly2Values() {
            // Assert
            assertThat(ChatSessionStatus.values()).hasSize(2);
        }

        @Test
        @DisplayName("should get value by name")
        void shouldGetValueByName() {
            // Act & Assert
            assertThat(ChatSessionStatus.valueOf("ACTIVE")).isEqualTo(ChatSessionStatus.ACTIVE);
            assertThat(ChatSessionStatus.valueOf("CLOSED")).isEqualTo(ChatSessionStatus.CLOSED);
        }

        @Test
        @DisplayName("ACTIVE should have correct name")
        void activeShouldHaveCorrectName() {
            // Assert
            assertThat(ChatSessionStatus.ACTIVE.name()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("CLOSED should have correct name")
        void closedShouldHaveCorrectName() {
            // Assert
            assertThat(ChatSessionStatus.CLOSED.name()).isEqualTo("CLOSED");
        }
    }

    @Nested
    @DisplayName("DomainConstants tests")
    class DomainConstantsTests {

        @Test
        @DisplayName("should have correct default session title")
        void shouldHaveCorrectDefaultSessionTitle() {
            // Assert
            assertThat(DomainConstants.DEFAULT_SESSION_TITLE).isEqualTo("New Chat");
        }

        @Test
        @DisplayName("should have correct max message length")
        void shouldHaveCorrectMaxMessageLength() {
            // Assert
            assertThat(DomainConstants.MAX_MESSAGE_LENGTH).isEqualTo(10000);
        }

        @Test
        @DisplayName("should have correct default recent messages count")
        void shouldHaveCorrectDefaultRecentMessagesCount() {
            // Assert
            assertThat(DomainConstants.DEFAULT_RECENT_MESSAGES_COUNT).isEqualTo(10);
        }

        @Test
        @DisplayName("should not have negative max message length")
        void shouldNotHaveNegativeMaxMessageLength() {
            // Assert
            assertThat(DomainConstants.MAX_MESSAGE_LENGTH).isGreaterThan(0);
        }

        @Test
        @DisplayName("should not have negative recent messages count")
        void shouldNotHaveNegativeRecentMessagesCount() {
            // Assert
            assertThat(DomainConstants.DEFAULT_RECENT_MESSAGES_COUNT).isGreaterThan(0);
        }

        @Test
        @DisplayName("should have reasonable max message length")
        void shouldHaveReasonableMaxMessageLength() {
            // Assert - reasonable max length should be at least 100
            assertThat(DomainConstants.MAX_MESSAGE_LENGTH).isGreaterThanOrEqualTo(100);
        }

        @Test
        @DisplayName("should have reasonable default recent messages count")
        void shouldHaveReasonableDefaultRecentMessagesCount() {
            // Assert - reasonable count should be between 1 and 100
            assertThat(DomainConstants.DEFAULT_RECENT_MESSAGES_COUNT)
                    .isGreaterThan(0)
                    .isLessThan(100);
        }

        @Test
        @DisplayName("should have non-null default session title")
        void shouldHaveNonNullDefaultSessionTitle() {
            // Assert
            assertThat(DomainConstants.DEFAULT_SESSION_TITLE).isNotNull();
        }

        @Test
        @DisplayName("should have non-empty default session title")
        void shouldHaveNonEmptyDefaultSessionTitle() {
            // Assert
            assertThat(DomainConstants.DEFAULT_SESSION_TITLE).isNotEmpty();
        }
    }
}
