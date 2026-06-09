package com.ai.tts.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpeechId Tests")
class SpeechIdTest {

    @Nested
    @DisplayName("generate factory method")
    class GenerateFactoryMethodTests {

        @Test
        @DisplayName("should generate unique UUID string")
        void shouldGenerateUniqueUuidString() {
            SpeechId id1 = SpeechId.generate();
            SpeechId id2 = SpeechId.generate();

            assertThat(id1.value()).isNotEqualTo(id2.value());
        }

        @Test
        @DisplayName("should generate valid UUID format")
        void shouldGenerateValidUuidFormat() {
            SpeechId id = SpeechId.generate();

            assertThat(id.value()).matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
            );
        }

        @Test
        @DisplayName("should generate non-null value")
        void shouldGenerateNonNullValue() {
            SpeechId id = SpeechId.generate();

            assertThat(id.value()).isNotNull();
            assertThat(id.value()).isNotBlank();
        }

        @Test
        @DisplayName("should generate different values on multiple calls")
        void shouldGenerateDifferentValuesOnMultipleCalls() {
            SpeechId id1 = SpeechId.generate();
            SpeechId id2 = SpeechId.generate();
            SpeechId id3 = SpeechId.generate();

            assertThat(id1.value()).isNotEqualTo(id2.value());
            assertThat(id2.value()).isNotEqualTo(id3.value());
            assertThat(id1.value()).isNotEqualTo(id3.value());
        }
    }

    @Nested
    @DisplayName("of factory method")
    class OfFactoryMethodTests {

        @Test
        @DisplayName("should return SpeechId with given value for valid input")
        void shouldReturnSpeechIdWithGivenValueForValidInput() {
            SpeechId id = SpeechId.of("valid-speech-id-123");

            assertThat(id.value()).isEqualTo("valid-speech-id-123");
        }

        @ParameterizedTest
        @ValueSource(strings = {"test-id", "another-id", "ID-456"})
        @DisplayName("should create SpeechId from various valid strings")
        void shouldCreateSpeechIdFromVariousValidStrings(String value) {
            SpeechId id = SpeechId.of(value);

            assertThat(id.value()).isEqualTo(value);
        }

        @Test
        @DisplayName("should return generated SpeechId for null input")
        void shouldReturnGeneratedSpeechIdForNullInput() {
            SpeechId id = SpeechId.of(null);

            assertThat(id.value()).isNotNull();
            assertThat(id.value()).matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should return generated SpeechId for blank input")
        void shouldReturnGeneratedSpeechIdForBlankInput(String value) {
            SpeechId id = SpeechId.of(value);

            assertThat(id.value()).isNotNull();
            assertThat(id.value()).isNotBlank();
            assertThat(id.value()).matches(
                    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
            );
        }

        @Test
        @DisplayName("should return different generated values for different null/blank inputs")
        void shouldReturnDifferentGeneratedValuesForDifferentNullBlankInputs() {
            SpeechId id1 = SpeechId.of(null);
            SpeechId id2 = SpeechId.of("");
            SpeechId id3 = SpeechId.of("   ");

            assertThat(id1.value()).isNotEqualTo(id2.value());
            assertThat(id2.value()).isNotEqualTo(id3.value());
        }
    }

    @Nested
    @DisplayName("record accessor")
    class RecordAccessorTests {

        @Test
        @DisplayName("should provide value accessor")
        void shouldProvideValueAccessor() {
            String expectedValue = "test-value";
            SpeechId id = SpeechId.of(expectedValue);

            assertThat(id.value()).isEqualTo(expectedValue);
        }
    }

    @Nested
    @DisplayName("immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("should be immutable record")
        void shouldBeImmutableRecord() {
            SpeechId id = SpeechId.of("immutable-test");

            assertThat(id.value()).isEqualTo("immutable-test");
            assertThat(id.value()).isInstanceOf(String.class);
        }
    }
}
