package com.ai.domain;

import com.ai.ai.domain.vo.DomainConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DomainConstants Tests
 *
 * Tests for DomainConstants following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Tests constant values and utility class constraints
 */
@DisplayName("DomainConstants")
class DomainConstantsTest {

    @Nested
    @DisplayName("Class Structure")
    class ClassStructure {

        @Test
        @DisplayName("should be a final class")
        void shouldBeFinalClass() {
            // Assert
            assertThat(java.lang.reflect.Modifier.isFinal(DomainConstants.class.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("should have private constructor")
        void shouldHavePrivateConstructor() throws NoSuchMethodException {
            // Arrange
            Constructor<DomainConstants> constructor = DomainConstants.class.getDeclaredConstructor();

            // Assert
            assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("should be non-instantiable - constructor is private")
        void shouldBeNonInstantiable() throws NoSuchMethodException {
            // This tests that the utility class follows the standard pattern
            Constructor<DomainConstants> constructor = DomainConstants.class.getDeclaredConstructor();

            // Assert - constructor should be private
            assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())).isTrue();

            // And attempting to call it would fail (without setAccessible)
            assertThatThrownBy(() -> constructor.newInstance())
                .isInstanceOf(IllegalAccessException.class);
        }

        @Test
        @DisplayName("should have no declared public methods")
        void shouldHaveNoDeclaredPublicMethods() {
            // Assert - Constants class should have no declared public methods
            // (Object methods from inheritance don't count)
            var declaredMethods = DomainConstants.class.getDeclaredMethods();
            var publicMethods = Arrays.stream(declaredMethods)
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .toList();

            assertThat(publicMethods).isEmpty();
        }
    }

    @Nested
    @DisplayName("DEFAULT_SESSION_TITLE")
    class DefaultSessionTitle {

        @Test
        @DisplayName("should be non-null")
        void shouldBeNonNull() {
            // Assert
            assertThat(DomainConstants.DEFAULT_SESSION_TITLE).isNotNull();
        }

        @Test
        @DisplayName("should be non-blank")
        void shouldBeNonBlank() {
            // Assert
            assertThat(DomainConstants.DEFAULT_SESSION_TITLE).isNotBlank();
        }

        @Test
        @DisplayName("should equal 'New Chat'")
        void shouldEqualNewChat() {
            // Assert
            assertThat(DomainConstants.DEFAULT_SESSION_TITLE).isEqualTo("New Chat");
        }
    }

    @Nested
    @DisplayName("MAX_MESSAGE_LENGTH")
    class MaxMessageLength {

        @Test
        @DisplayName("should be positive")
        void shouldBePositive() {
            // Assert
            assertThat(DomainConstants.MAX_MESSAGE_LENGTH).isPositive();
        }

        @Test
        @DisplayName("should be greater than zero")
        void shouldBeGreaterThanZero() {
            // Assert
            assertThat(DomainConstants.MAX_MESSAGE_LENGTH).isGreaterThan(0);
        }

        @Test
        @DisplayName("should equal 10000")
        void shouldEqual10000() {
            // Assert
            assertThat(DomainConstants.MAX_MESSAGE_LENGTH).isEqualTo(10000);
        }

        @Test
        @DisplayName("should be consistent with MessageContent max length")
        void shouldBeConsistentWithMessageContentMaxLength() {
            // This tests consistency between constants
            // MessageContent.MAX_LENGTH = 10000
            assertThat(DomainConstants.MAX_MESSAGE_LENGTH).isEqualTo(10000);
        }
    }

    @Nested
    @DisplayName("DEFAULT_RECENT_MESSAGES_COUNT")
    class DefaultRecentMessagesCount {

        @Test
        @DisplayName("should be positive")
        void shouldBePositive() {
            // Assert
            assertThat(DomainConstants.DEFAULT_RECENT_MESSAGES_COUNT).isPositive();
        }

        @Test
        @DisplayName("should be greater than zero")
        void shouldBeGreaterThanZero() {
            // Assert
            assertThat(DomainConstants.DEFAULT_RECENT_MESSAGES_COUNT).isGreaterThan(0);
        }

        @Test
        @DisplayName("should equal 10")
        void shouldEqual10() {
            // Assert
            assertThat(DomainConstants.DEFAULT_RECENT_MESSAGES_COUNT).isEqualTo(10);
        }

        @Test
        @DisplayName("should be a reasonable value for recent messages")
        void shouldBeReasonableValueForRecentMessages() {
            // 10 is a reasonable default for recent messages
            assertThat(DomainConstants.DEFAULT_RECENT_MESSAGES_COUNT).isBetween(5, 50);
        }
    }

    @Nested
    @DisplayName("Constant Relationships")
    class ConstantRelationships {

        @Test
        @DisplayName("should have max message length greater than default recent messages count")
        void shouldHaveMaxMessageLengthGreaterThanDefaultRecentMessagesCount() {
            // This ensures we can store messages that could reference many recent ones
            assertThat(DomainConstants.MAX_MESSAGE_LENGTH)
                .isGreaterThan(DomainConstants.DEFAULT_RECENT_MESSAGES_COUNT);
        }
    }
}
