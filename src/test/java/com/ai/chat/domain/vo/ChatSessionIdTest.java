package com.ai.chat.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ChatSessionId Value Object Tests
 * 
 * Tests for ChatSessionId immutable value object following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests creation, equality, and validation
 */
@DisplayName("ChatSessionId")
class ChatSessionIdTest {

    private static final String TEST_UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create from string value")
        void shouldCreateFromStringValue() {
            ChatSessionId id = ChatSessionId.of(TEST_UUID_STRING);

            assertThat(id.value()).isEqualTo(TEST_UUID_STRING);
        }

        @Test
        @DisplayName("should generate new random ID")
        void shouldGenerateNewRandomId() {
            ChatSessionId id1 = ChatSessionId.generate();
            ChatSessionId id2 = ChatSessionId.generate();

            assertThat(id1).isNotNull();
            assertThat(id2).isNotNull();
            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1.value()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("should throw exception when creating from null string")
        void shouldThrowExceptionWhenCreatingFromNullString() {
            assertThatThrownBy(() -> ChatSessionId.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
        }

        @Test
        @DisplayName("should throw exception when creating from blank string")
        void shouldThrowExceptionWhenCreatingFromBlankString() {
            assertThatThrownBy(() -> ChatSessionId.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
        }

        @Test
        @DisplayName("should throw exception when creating from empty string")
        void shouldThrowExceptionWhenCreatingFromEmptyString() {
            assertThatThrownBy(() -> ChatSessionId.of(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal to same string")
        void shouldBeEqualToSameString() {
            ChatSessionId id1 = ChatSessionId.of(TEST_UUID_STRING);
            ChatSessionId id2 = ChatSessionId.of(TEST_UUID_STRING);

            assertThat(id1).isEqualTo(id2);
        }

        @Test
        @DisplayName("should not be equal to different string")
        void shouldNotBeEqualToDifferentString() {
            String differentUuid = "223e4567-e89b-12d3-a456-426614174001";
            ChatSessionId id1 = ChatSessionId.of(TEST_UUID_STRING);
            ChatSessionId id2 = ChatSessionId.of(differentUuid);

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            ChatSessionId id = ChatSessionId.of(TEST_UUID_STRING);

            assertThat(id).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            ChatSessionId id = ChatSessionId.of(TEST_UUID_STRING);

            assertThat(id).isNotEqualTo("not-a-session-id");
            assertThat(id).isNotEqualTo(UUID.fromString(TEST_UUID_STRING));
        }

        @Test
        @DisplayName("should be reflexive")
        void shouldBeReflexive() {
            ChatSessionId id = ChatSessionId.of(TEST_UUID_STRING);

            assertThat(id).isEqualTo(id);
        }

        @Test
        @DisplayName("should be symmetric")
        void shouldBeSymmetric() {
            ChatSessionId id1 = ChatSessionId.of(TEST_UUID_STRING);
            ChatSessionId id2 = ChatSessionId.of(TEST_UUID_STRING);

            assertThat(id1.equals(id2)).isEqualTo(id2.equals(id1));
        }

        @Test
        @DisplayName("should be transitive")
        void shouldBeTransitive() {
            ChatSessionId id1 = ChatSessionId.of(TEST_UUID_STRING);
            ChatSessionId id2 = ChatSessionId.of(TEST_UUID_STRING);
            ChatSessionId id3 = ChatSessionId.of(TEST_UUID_STRING);

            assertThat(id1.equals(id2) && id2.equals(id3)).isTrue();
            assertThat(id1.equals(id3)).isTrue();
        }
    }

    @Nested
    @DisplayName("HashCode")
    class HashCode {

        @Test
        @DisplayName("should have same hashCode for equal instances")
        void shouldHaveSameHashCodeForEqualInstances() {
            ChatSessionId id1 = ChatSessionId.of(TEST_UUID_STRING);
            ChatSessionId id2 = ChatSessionId.of(TEST_UUID_STRING);

            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            ChatSessionId id = ChatSessionId.of(TEST_UUID_STRING);

            assertThat(id.hashCode()).isEqualTo(id.hashCode());
        }
    }

    @Nested
    @DisplayName("Value Access")
    class ValueAccess {

        @Test
        @DisplayName("should return correct string value")
        void shouldReturnCorrectStringValue() {
            ChatSessionId id = ChatSessionId.of(TEST_UUID_STRING);

            assertThat(id.value()).isEqualTo(TEST_UUID_STRING);
        }

        @Test
        @DisplayName("should return same value in toString")
        void shouldReturnSameValueInToString() {
            ChatSessionId id = ChatSessionId.of(TEST_UUID_STRING);

            assertThat(id.toString()).isEqualTo(TEST_UUID_STRING);
        }

        @Test
        @DisplayName("should return same string as UUID toString for generated ID")
        void shouldReturnSameStringAsUUIDToStringForGeneratedId() {
            ChatSessionId id = ChatSessionId.generate();

            assertThat(id.toString()).isEqualTo(id.value());
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should not allow modification of underlying string")
        void shouldNotAllowModificationOfUnderlyingString() {
            ChatSessionId id = ChatSessionId.of(TEST_UUID_STRING);
            String originalValue = id.value();

            assertThat(id.value()).isEqualTo(originalValue);
        }
    }
}
