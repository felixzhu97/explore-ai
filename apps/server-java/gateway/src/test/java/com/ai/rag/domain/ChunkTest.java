package com.ai.rag.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Chunk Tests")
class ChunkTest {

    @Nested
    @DisplayName("constructor validation")
    class ConstructorValidationTests {

        @Test
        @DisplayName("should create chunk with valid parameters")
        void shouldCreateChunkWithValidParameters() {
            DocumentId docId = DocumentId.generate();

            Chunk chunk = new Chunk("Valid chunk text", 0, docId);

            assertThat(chunk.text()).isEqualTo("Valid chunk text");
            assertThat(chunk.position()).isZero();
            assertThat(chunk.sourceDocumentId()).isEqualTo(docId);
        }

        @Test
        @DisplayName("should throw exception when text is null")
        void shouldThrowExceptionWhenTextIsNull() {
            DocumentId docId = DocumentId.generate();

            assertThatThrownBy(() -> new Chunk(null, 0, docId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be blank");
        }

        @Test
        @DisplayName("should throw exception when text is empty")
        void shouldThrowExceptionWhenTextIsEmpty() {
            DocumentId docId = DocumentId.generate();

            assertThatThrownBy(() -> new Chunk("", 0, docId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be blank");
        }

        @Test
        @DisplayName("should throw exception when text is whitespace")
        void shouldThrowExceptionWhenTextIsWhitespace() {
            DocumentId docId = DocumentId.generate();

            assertThatThrownBy(() -> new Chunk("   ", 0, docId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be blank");
        }

        @Test
        @DisplayName("should throw exception when position is negative")
        void shouldThrowExceptionWhenPositionIsNegative() {
            DocumentId docId = DocumentId.generate();

            assertThatThrownBy(() -> new Chunk("Text", -1, docId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be negative");
        }

        @Test
        @DisplayName("should throw exception when sourceDocumentId is null")
        void shouldThrowExceptionWhenSourceDocumentIdIsNull() {
            assertThatThrownBy(() -> new Chunk("Text", 0, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("source document ID");
        }
    }

    @Nested
    @DisplayName("create factory method")
    class CreateFactoryMethodTests {

        @Test
        @DisplayName("should create chunk using factory method")
        void shouldCreateChunkUsingFactoryMethod() {
            DocumentId docId = DocumentId.generate();

            Chunk chunk = Chunk.create("Factory chunk", 5, docId);

            assertThat(chunk.text()).isEqualTo("Factory chunk");
            assertThat(chunk.position()).isEqualTo(5);
            assertThat(chunk.sourceDocumentId()).isEqualTo(docId);
        }

        @Test
        @DisplayName("should delegate to constructor with validation")
        void shouldDelegateToConstructorWithValidation() {
            DocumentId docId = DocumentId.generate();

            assertThatThrownBy(() -> Chunk.create(null, 0, docId))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("length method")
    class LengthMethodTests {

        @Test
        @DisplayName("should return correct text length")
        void shouldReturnCorrectTextLength() {
            DocumentId docId = DocumentId.generate();
            String text = "Hello, World!";
            Chunk chunk = new Chunk(text, 0, docId);

            assertThat(chunk.length()).isEqualTo(text.length());
        }

        @Test
        @DisplayName("should return correct length")
        void shouldReturnCorrectLength() {
            DocumentId docId = DocumentId.generate();
            Chunk chunk = new Chunk("x", 0, docId);

            assertThat(chunk.length()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("should include position and length in toString")
        void shouldIncludePositionAndLengthInToString() {
            DocumentId docId = DocumentId.generate();
            Chunk chunk = new Chunk("Test content", 3, docId);

            String result = chunk.toString();

            assertThat(result).contains("position=3");
            assertThat(result).contains("length=12");
        }
    }

    @Nested
    @DisplayName("immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("should be immutable record")
        void shouldBeImmutableRecord() {
            DocumentId docId = DocumentId.generate();
            Chunk chunk = new Chunk("Original", 0, docId);

            assertThat(chunk.text()).isEqualTo("Original");
            assertThat(chunk.position()).isZero();
            assertThat(chunk.sourceDocumentId()).isEqualTo(docId);
        }

        @Test
        @DisplayName("should maintain state after creation")
        void shouldMaintainStateAfterCreation() {
            DocumentId docId = DocumentId.generate();
            Chunk chunk = Chunk.create("Immutable text", 10, docId);

            assertThat(chunk.text()).isEqualTo("Immutable text");
            assertThat(chunk.position()).isEqualTo(10);
        }
    }
}
