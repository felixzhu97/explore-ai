package com.ai.domain;

import com.ai.domain.vo.MessageContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MessageContent Value Object Tests
 * 
 * Tests for MessageContent following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests validation rules and behavior
 */
@DisplayName("MessageContent")
class MessageContentTest {

    @Nested
    @DisplayName("Creation with single parameter")
    class CreationWithSingleParameter {

        @Test
        @DisplayName("should create message content with default role user")
        void shouldCreateMessageContentWithDefaultRoleUser() {
            // Arrange
            String text = "Hello, world!";

            // Act
            MessageContent content = new MessageContent(text);

            // Assert
            assertThat(content.text()).isEqualTo("Hello, world!");
            assertThat(content.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("should create message content and trim whitespace")
        void shouldCreateMessageContentAndTrimWhitespace() {
            // Arrange
            String text = "  Hello, world!  ";

            // Act
            MessageContent content = new MessageContent(text);

            // Assert
            assertThat(content.text()).isEqualTo("Hello, world!");
        }

        @Test
        @DisplayName("should throw exception when text is null")
        void shouldThrowExceptionWhenTextIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new MessageContent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Message content cannot be null or blank");
        }

        @Test
        @DisplayName("should throw exception when text is blank")
        void shouldThrowExceptionWhenTextIsBlank() {
            // Act & Assert
            assertThatThrownBy(() -> new MessageContent("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Message content cannot be null or blank");
        }

        @Test
        @DisplayName("should throw exception when text exceeds maximum length")
        void shouldThrowExceptionWhenTextExceedsMaximumLength() {
            // Arrange
            String text = "a".repeat(10001);

            // Act & Assert
            assertThatThrownBy(() -> new MessageContent(text))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum length");
        }

        @Test
        @DisplayName("should allow text with exactly maximum length")
        void shouldAllowTextWithExactlyMaximumLength() {
            // Arrange
            String text = "a".repeat(10000);

            // Act
            MessageContent content = new MessageContent(text);

            // Assert
            assertThat(content.text()).hasSize(10000);
        }

        @Test
        @DisplayName("should allow text with exactly minimum length")
        void shouldAllowTextWithExactlyMinimumLength() {
            // Arrange
            String text = "a";

            // Act
            MessageContent content = new MessageContent(text);

            // Assert
            assertThat(content.text()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Creation with text and role")
    class CreationWithTextAndRole {

        @Test
        @DisplayName("should create message content with user role")
        void shouldCreateMessageContentWithUserRole() {
            // Arrange
            String text = "Hello";
            String role = "user";

            // Act
            MessageContent content = new MessageContent(text, role);

            // Assert
            assertThat(content.text()).isEqualTo("Hello");
            assertThat(content.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("should create message content with assistant role")
        void shouldCreateMessageContentWithAssistantRole() {
            // Arrange
            String text = "Hello";
            String role = "assistant";

            // Act
            MessageContent content = new MessageContent(text, role);

            // Assert
            assertThat(content.text()).isEqualTo("Hello");
            assertThat(content.role()).isEqualTo("assistant");
        }

        @Test
        @DisplayName("should normalize role to lowercase")
        void shouldNormalizeRoleToLowercase() {
            // Arrange
            String text = "Hello";
            String role = "USER";

            // Act
            MessageContent content = new MessageContent(text, role);

            // Assert
            assertThat(content.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("should normalize role to lowercase for assistant")
        void shouldNormalizeRoleToLowercaseForAssistant() {
            // Arrange
            String text = "Hello";
            String role = "Assistant";

            // Act
            MessageContent content = new MessageContent(text, role);

            // Assert
            assertThat(content.role()).isEqualTo("assistant");
        }

        @Test
        @DisplayName("should trim role whitespace")
        void shouldTrimRoleWhitespace() {
            // Arrange
            String text = "Hello";
            String role = "  user  ";

            // Act
            MessageContent content = new MessageContent(text, role);

            // Assert
            assertThat(content.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("should use default user role when role is null")
        void shouldUseDefaultUserRoleWhenRoleIsNull() {
            // Arrange
            String text = "Hello";

            // Act
            MessageContent content = new MessageContent(text, null);

            // Assert
            assertThat(content.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("should use default user role when role is blank")
        void shouldUseDefaultUserRoleWhenRoleIsBlank() {
            // Arrange
            String text = "Hello";
            String role = "   ";

            // Act
            MessageContent content = new MessageContent(text, role);

            // Assert
            assertThat(content.role()).isEqualTo("user");
        }

        @Test
        @DisplayName("should throw exception when role is invalid")
        void shouldThrowExceptionWhenRoleIsInvalid() {
            // Arrange
            String text = "Hello";
            String role = "admin";

            // Act & Assert
            assertThatThrownBy(() -> new MessageContent(text, role))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role must be either 'user' or 'assistant'");
        }
    }

    @Nested
    @DisplayName("isFromUser")
    class IsFromUser {

        @Test
        @DisplayName("should return true when role is user")
        void shouldReturnTrueWhenRoleIsUser() {
            // Arrange
            MessageContent content = new MessageContent("Hello", "user");

            // Act & Assert
            assertThat(content.isFromUser()).isTrue();
        }

        @Test
        @DisplayName("should return false when role is assistant")
        void shouldReturnFalseWhenRoleIsAssistant() {
            // Arrange
            MessageContent content = new MessageContent("Hello", "assistant");

            // Act & Assert
            assertThat(content.isFromUser()).isFalse();
        }
    }

    @Nested
    @DisplayName("isFromAssistant")
    class IsFromAssistant {

        @Test
        @DisplayName("should return true when role is assistant")
        void shouldReturnTrueWhenRoleIsAssistant() {
            // Arrange
            MessageContent content = new MessageContent("Hello", "assistant");

            // Act & Assert
            assertThat(content.isFromAssistant()).isTrue();
        }

        @Test
        @DisplayName("should return false when role is user")
        void shouldReturnFalseWhenRoleIsUser() {
            // Arrange
            MessageContent content = new MessageContent("Hello", "user");

            // Act & Assert
            assertThat(content.isFromAssistant()).isFalse();
        }
    }

    @Nested
    @DisplayName("withRole")
    class WithRole {

        @Test
        @DisplayName("should return new instance with changed role")
        void shouldReturnNewInstanceWithChangedRole() {
            // Arrange
            MessageContent content = new MessageContent("Hello", "user");

            // Act
            MessageContent newContent = content.withRole("assistant");

            // Assert
            assertThat(content.role()).isEqualTo("user");
            assertThat(newContent.role()).isEqualTo("assistant");
            assertThat(content.text()).isEqualTo(newContent.text());
        }

        @Test
        @DisplayName("should return new instance with same text")
        void shouldReturnNewInstanceWithSameText() {
            // Arrange
            MessageContent content = new MessageContent("Hello, world!", "user");

            // Act
            MessageContent newContent = content.withRole("assistant");

            // Assert
            assertThat(newContent.text()).isEqualTo("Hello, world!");
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal when text and role are the same")
        void shouldBeEqualWhenTextAndRoleAreTheSame() {
            // Arrange
            MessageContent content1 = new MessageContent("Hello", "user");
            MessageContent content2 = new MessageContent("Hello", "user");

            // Assert
            assertThat(content1).isEqualTo(content2);
            assertThat(content1.hashCode()).isEqualTo(content2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when text is different")
        void shouldNotBeEqualWhenTextIsDifferent() {
            // Arrange
            MessageContent content1 = new MessageContent("Hello", "user");
            MessageContent content2 = new MessageContent("World", "user");

            // Assert
            assertThat(content1).isNotEqualTo(content2);
        }

        @Test
        @DisplayName("should not be equal when role is different")
        void shouldNotBeEqualWhenRoleIsDifferent() {
            // Arrange
            MessageContent content1 = new MessageContent("Hello", "user");
            MessageContent content2 = new MessageContent("Hello", "assistant");

            // Assert
            assertThat(content1).isNotEqualTo(content2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should include role in toString")
        void shouldIncludeRoleInToString() {
            // Arrange
            MessageContent content = new MessageContent("Hello", "user");

            // Act
            String result = content.toString();

            // Assert
            assertThat(result).contains("role='user'");
        }

        @Test
        @DisplayName("should truncate long text in toString")
        void shouldTruncateLongTextInToString() {
            // Arrange
            String longText = "a".repeat(100);
            MessageContent content = new MessageContent(longText, "user");

            // Act
            String result = content.toString();

            // Assert
            assertThat(result).doesNotContain(longText);
            assertThat(result).contains("...");
        }
    }
}
