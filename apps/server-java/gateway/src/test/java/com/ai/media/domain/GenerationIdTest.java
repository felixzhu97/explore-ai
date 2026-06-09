package com.ai.media.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GenerationId Tests")
class GenerationIdTest {

    @Nested
    @DisplayName("generate factory method")
    class GenerateFactoryMethodTests {

        @Test
        @DisplayName("should generate unique UUID string")
        void shouldGenerateUniqueUuidString() {
            GenerationId id1 = GenerationId.generate();
            GenerationId id2 = GenerationId.generate();

            assertThat(id1.value()).isNotEqualTo(id2.value());
        }

        @Test
        @DisplayName("should generate valid UUID format")
        void shouldGenerateValidUuidFormat() {
            GenerationId id = GenerationId.generate();

            assertThat(id.value()).matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
            );
        }

        @Test
        @DisplayName("should generate non-null value")
        void shouldGenerateNonNullValue() {
            GenerationId id = GenerationId.generate();

            assertThat(id.value()).isNotNull();
            assertThat(id.value()).isNotBlank();
        }

        @Test
        @DisplayName("should generate different values on multiple calls")
        void shouldGenerateDifferentValuesOnMultipleCalls() {
            GenerationId id1 = GenerationId.generate();
            GenerationId id2 = GenerationId.generate();
            GenerationId id3 = GenerationId.generate();

            assertThat(id1.value()).isNotEqualTo(id2.value());
            assertThat(id2.value()).isNotEqualTo(id3.value());
            assertThat(id1.value()).isNotEqualTo(id3.value());
        }
    }

    @Nested
    @DisplayName("of factory method")
    class OfFactoryMethodTests {

        @Test
        @DisplayName("should create GenerationId from valid UUID string")
        void shouldCreateGenerationIdFromValidUuidString() {
            String uuidString = "123e4567-e89b-12d3-a456-426614174000";
            GenerationId id = GenerationId.of(uuidString);

            assertThat(id.value()).isEqualTo(uuidString);
        }

        @ParameterizedTest
        @ValueSource(strings = {"valid-id", "another-id-123", "ID-456"})
        @DisplayName("should create GenerationId from various valid strings")
        void shouldCreateGenerationIdFromVariousValidStrings(String value) {
            GenerationId id = GenerationId.of(value);

            assertThat(id.value()).isEqualTo(value);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should throw exception for null or blank input")
        void shouldThrowExceptionForNullOrBlankInput(String value) {
            assertThatThrownBy(() -> GenerationId.of(value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("value method")
    class ValueMethodTests {

        @Test
        @DisplayName("should return the stored value")
        void shouldReturnTheStoredValue() {
            String expectedValue = "test-uuid-value";
            GenerationId id = GenerationId.of(expectedValue);

            assertThat(id.value()).isEqualTo(expectedValue);
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("should be equal when values match")
        void shouldBeEqualWhenValuesMatch() {
            String value = "same-uuid";
            GenerationId id1 = GenerationId.of(value);
            GenerationId id2 = GenerationId.of(value);

            assertThat(id1).isEqualTo(id2);
        }

        @Test
        @DisplayName("should not be equal when values differ")
        void shouldNotBeEqualWhenValuesDiffer() {
            GenerationId id1 = GenerationId.of("uuid-one");
            GenerationId id2 = GenerationId.of("uuid-two");

            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            GenerationId id = GenerationId.of("test-uuid");

            assertThat(id).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            GenerationId id = GenerationId.of("test-uuid");

            assertThat(id).isNotEqualTo("test-uuid");
        }

        @Test
        @DisplayName("should have same hashCode when equal")
        void shouldHaveSameHashCodeWhenEqual() {
            String value = "hash-test-uuid";
            GenerationId id1 = GenerationId.of(value);
            GenerationId id2 = GenerationId.of(value);

            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("should return value as string")
        void shouldReturnValueAsString() {
            String value = "to-string-test";
            GenerationId id = GenerationId.of(value);

            assertThat(id.toString()).isEqualTo(value);
        }
    }
}
