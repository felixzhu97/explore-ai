package com.ai.domain;

import com.ai.ai.domain.vo.ChatSessionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChatSessionId Value Object Tests
 * 
 * Tests for ChatSessionId following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests validation rules and behavior
 */
@DisplayName("ChatSessionId")
class ChatSessionIdTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create ChatSessionId with valid value")
        void shouldCreateChatSessionIdWithValidValue() {
            // Arrange
            String value = UUID.randomUUID().toString();

            // Act
            ChatSessionId sessionId = new ChatSessionId(value);

            // Assert
            assertThat(sessionId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("should throw exception when value is null")
        void shouldThrowExceptionWhenValueIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new ChatSessionId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ChatSessionId cannot be null or blank");
        }

        @Test
        @DisplayName("should throw exception when value is blank")
        void shouldThrowExceptionWhenValueIsBlank() {
            // Act & Assert
            assertThatThrownBy(() -> new ChatSessionId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ChatSessionId cannot be null or blank");
        }

        @Test
        @DisplayName("should throw exception when value is empty")
        void shouldThrowExceptionWhenValueIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> new ChatSessionId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ChatSessionId cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("of factory method")
    class OfFactoryMethod {

        @Test
        @DisplayName("should create ChatSessionId using of method")
        void shouldCreateChatSessionIdUsingOfMethod() {
            // Arrange
            String value = UUID.randomUUID().toString();

            // Act
            ChatSessionId sessionId = ChatSessionId.of(value);

            // Assert
            assertThat(sessionId.value()).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("generate factory method")
    class GenerateFactoryMethod {

        @Test
        @DisplayName("should generate ChatSessionId with UUID format")
        void shouldGenerateChatSessionIdWithUuidFormat() {
            // Act
            ChatSessionId sessionId = ChatSessionId.generate();

            // Assert
            assertThat(sessionId.value()).isNotNull();
            assertThat(UUID.fromString(sessionId.value())).isNotNull();
        }

        @Test
        @DisplayName("should generate unique ChatSessionIds")
        void shouldGenerateUniqueChatSessionIds() {
            // Act
            ChatSessionId sessionId1 = ChatSessionId.generate();
            ChatSessionId sessionId2 = ChatSessionId.generate();

            // Assert
            assertThat(sessionId1).isNotEqualTo(sessionId2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should return the underlying value in toString")
        void shouldReturnTheUnderlyingValueInToString() {
            // Arrange
            String value = UUID.randomUUID().toString();
            ChatSessionId sessionId = ChatSessionId.of(value);

            // Act
            String result = sessionId.toString();

            // Assert
            assertThat(result).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal when values are the same")
        void shouldBeEqualWhenValuesAreTheSame() {
            // Arrange
            String value = UUID.randomUUID().toString();
            ChatSessionId sessionId1 = ChatSessionId.of(value);
            ChatSessionId sessionId2 = ChatSessionId.of(value);

            // Assert
            assertThat(sessionId1).isEqualTo(sessionId2);
            assertThat(sessionId1.hashCode()).isEqualTo(sessionId2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when values are different")
        void shouldNotBeEqualWhenValuesAreDifferent() {
            // Arrange
            ChatSessionId sessionId1 = ChatSessionId.of(UUID.randomUUID().toString());
            ChatSessionId sessionId2 = ChatSessionId.of(UUID.randomUUID().toString());

            // Assert
            assertThat(sessionId1).isNotEqualTo(sessionId2);
        }

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            // Arrange
            ChatSessionId sessionId = ChatSessionId.of(UUID.randomUUID().toString());

            // Assert
            assertThat(sessionId).isEqualTo(sessionId);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Arrange
            ChatSessionId sessionId = ChatSessionId.of(UUID.randomUUID().toString());

            // Assert
            assertThat(sessionId).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            // Arrange
            ChatSessionId sessionId = ChatSessionId.of(UUID.randomUUID().toString());
            String rawString = UUID.randomUUID().toString();

            // Assert
            assertThat(sessionId).isNotEqualTo(rawString);
        }
    }
}
