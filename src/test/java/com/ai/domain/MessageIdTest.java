package com.ai.domain;

import com.ai.ai.domain.vo.MessageId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MessageId Value Object Tests
 * 
 * Tests for MessageId following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests validation rules and behavior
 */
@DisplayName("MessageId")
class MessageIdTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create MessageId with valid value")
        void shouldCreateMessageIdWithValidValue() {
            // Arrange
            String value = UUID.randomUUID().toString();

            // Act
            MessageId messageId = new MessageId(value);

            // Assert
            assertThat(messageId.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("should throw exception when value is null")
        void shouldThrowExceptionWhenValueIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new MessageId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MessageId cannot be null or blank");
        }

        @Test
        @DisplayName("should throw exception when value is blank")
        void shouldThrowExceptionWhenValueIsBlank() {
            // Act & Assert
            assertThatThrownBy(() -> new MessageId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MessageId cannot be null or blank");
        }

        @Test
        @DisplayName("should throw exception when value is empty")
        void shouldThrowExceptionWhenValueIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> new MessageId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MessageId cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("of factory method")
    class OfFactoryMethod {

        @Test
        @DisplayName("should create MessageId using of method")
        void shouldCreateMessageIdUsingOfMethod() {
            // Arrange
            String value = UUID.randomUUID().toString();

            // Act
            MessageId messageId = MessageId.of(value);

            // Assert
            assertThat(messageId.value()).isEqualTo(value);
        }
    }

    @Nested
    @DisplayName("generate factory method")
    class GenerateFactoryMethod {

        @Test
        @DisplayName("should generate MessageId with UUID format")
        void shouldGenerateMessageIdWithUuidFormat() {
            // Act
            MessageId messageId = MessageId.generate();

            // Assert
            assertThat(messageId.value()).isNotNull();
            assertThat(UUID.fromString(messageId.value())).isNotNull();
        }

        @Test
        @DisplayName("should generate unique MessageIds")
        void shouldGenerateUniqueMessageIds() {
            // Act
            MessageId messageId1 = MessageId.generate();
            MessageId messageId2 = MessageId.generate();

            // Assert
            assertThat(messageId1).isNotEqualTo(messageId2);
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
            MessageId messageId = MessageId.of(value);

            // Act
            String result = messageId.toString();

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
            MessageId messageId1 = MessageId.of(value);
            MessageId messageId2 = MessageId.of(value);

            // Assert
            assertThat(messageId1).isEqualTo(messageId2);
            assertThat(messageId1.hashCode()).isEqualTo(messageId2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when values are different")
        void shouldNotBeEqualWhenValuesAreDifferent() {
            // Arrange
            MessageId messageId1 = MessageId.of(UUID.randomUUID().toString());
            MessageId messageId2 = MessageId.of(UUID.randomUUID().toString());

            // Assert
            assertThat(messageId1).isNotEqualTo(messageId2);
        }

        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            // Arrange
            MessageId messageId = MessageId.of(UUID.randomUUID().toString());

            // Assert
            assertThat(messageId).isEqualTo(messageId);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Arrange
            MessageId messageId = MessageId.of(UUID.randomUUID().toString());

            // Assert
            assertThat(messageId).isNotEqualTo(null);
        }
    }
}
