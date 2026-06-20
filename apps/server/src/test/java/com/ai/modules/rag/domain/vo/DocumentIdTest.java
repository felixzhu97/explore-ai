package com.ai.modules.rag.domain.vo;

import com.ai.modules.rag.domain.vo.DocumentId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DocumentId Value Object Tests
 * 
 * Tests for DocumentId immutable value object following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests creation, equality, and validation
 */
@DisplayName("DocumentId")
class DocumentIdTest {

    private static final UUID TEST_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final String TEST_UUID_STRING = "123e4567-e89b-12d3-a456-426614174000";

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create from UUID")
        void shouldCreateFromUUID() {
            // Act
            DocumentId documentId = DocumentId.of(TEST_UUID);

            // Assert
            assertThat(documentId.value()).isEqualTo(TEST_UUID);
        }

        @Test
        @DisplayName("should create from string")
        void shouldCreateFromString() {
            // Act
            DocumentId documentId = DocumentId.of(TEST_UUID_STRING);

            // Assert
            assertThat(documentId.value()).isEqualTo(TEST_UUID);
        }

        @Test
        @DisplayName("should generate new random ID")
        void shouldGenerateNewRandomId() {
            // Act
            DocumentId id1 = DocumentId.generate();
            DocumentId id2 = DocumentId.generate();

            // Assert
            assertThat(id1).isNotNull();
            assertThat(id2).isNotNull();
            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("should throw exception when creating from null UUID")
        void shouldThrowExceptionWhenCreatingFromNullUUID() {
            // Act & Assert
            assertThatThrownBy(() -> DocumentId.of((UUID) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("UUID cannot be null");
        }

        @Test
        @DisplayName("should throw exception when creating from null string")
        void shouldThrowExceptionWhenCreatingFromNullString() {
            // Act & Assert
            assertThatThrownBy(() -> DocumentId.of((String) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("UUID string cannot be null or blank");
        }

        @Test
        @DisplayName("should throw exception when creating from blank string")
        void shouldThrowExceptionWhenCreatingFromBlankString() {
            // Act & Assert
            assertThatThrownBy(() -> DocumentId.of("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("UUID string cannot be null or blank");
        }

        @Test
        @DisplayName("should throw exception when creating from invalid UUID string")
        void shouldThrowExceptionWhenCreatingFromInvalidUUIDString() {
            // Act & Assert
            assertThatThrownBy(() -> DocumentId.of("not-a-valid-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal to same UUID")
        void shouldBeEqualToSameUUID() {
            // Arrange
            DocumentId id1 = DocumentId.of(TEST_UUID);
            DocumentId id2 = DocumentId.of(TEST_UUID);

            // Assert
            assertThat(id1).isEqualTo(id2);
        }

        @Test
        @DisplayName("should be equal to different UUID with same string")
        void shouldBeEqualToDifferentUUIDWithSameString() {
            // Arrange
            DocumentId id1 = DocumentId.of(TEST_UUID_STRING);
            DocumentId id2 = DocumentId.of(TEST_UUID_STRING);

            // Assert
            assertThat(id1).isEqualTo(id2);
        }

        @Test
        @DisplayName("should not be equal to different UUID")
        void shouldNotBeEqualToDifferentUUID() {
            // Arrange
            UUID differentUUID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
            DocumentId id1 = DocumentId.of(TEST_UUID);
            DocumentId id2 = DocumentId.of(differentUUID);

            // Assert
            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            // Arrange
            DocumentId id = DocumentId.of(TEST_UUID);

            // Assert
            assertThat(id).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            // Arrange
            DocumentId id = DocumentId.of(TEST_UUID);

            // Assert
            assertThat(id).isNotEqualTo("not-a-document-id");
            assertThat(id).isNotEqualTo(TEST_UUID);
        }

        @Test
        @DisplayName("should be reflexive")
        void shouldBeReflexive() {
            // Arrange
            DocumentId id = DocumentId.of(TEST_UUID);

            // Assert
            assertThat(id).isEqualTo(id);
        }

        @Test
        @DisplayName("should be symmetric")
        void shouldBeSymmetric() {
            // Arrange
            DocumentId id1 = DocumentId.of(TEST_UUID);
            DocumentId id2 = DocumentId.of(TEST_UUID);

            // Assert
            assertThat(id1.equals(id2)).isEqualTo(id2.equals(id1));
        }

        @Test
        @DisplayName("should be transitive")
        void shouldBeTransitive() {
            // Arrange
            DocumentId id1 = DocumentId.of(TEST_UUID);
            DocumentId id2 = DocumentId.of(TEST_UUID);
            DocumentId id3 = DocumentId.of(TEST_UUID);

            // Assert
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
            // Arrange
            DocumentId id1 = DocumentId.of(TEST_UUID);
            DocumentId id2 = DocumentId.of(TEST_UUID);

            // Assert
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            // Arrange
            DocumentId id = DocumentId.of(TEST_UUID);

            // Assert
            assertThat(id.hashCode()).isEqualTo(id.hashCode());
        }
    }

    @Nested
    @DisplayName("Value Access")
    class ValueAccess {

        @Test
        @DisplayName("should return correct UUID value")
        void shouldReturnCorrectUUIDValue() {
            // Act
            DocumentId id = DocumentId.of(TEST_UUID);

            // Assert
            assertThat(id.value()).isEqualTo(TEST_UUID);
            assertThat(id.value()).isInstanceOf(UUID.class);
        }

        @Test
        @DisplayName("should return correct string value")
        void shouldReturnCorrectStringValue() {
            // Act
            DocumentId id = DocumentId.of(TEST_UUID);

            // Assert
            assertThat(id.toString()).isEqualTo(TEST_UUID_STRING);
        }

        @Test
        @DisplayName("should return same string as UUID toString")
        void shouldReturnSameStringAsUUIDToString() {
            // Arrange
            UUID uuid = UUID.randomUUID();
            DocumentId id = DocumentId.of(uuid);

            // Assert
            assertThat(id.toString()).isEqualTo(uuid.toString());
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("should not allow modification of underlying UUID")
        void shouldNotAllowModificationOfUnderlyingUUID() {
            // Arrange
            DocumentId id = DocumentId.of(TEST_UUID);
            UUID originalValue = id.value();

            // Assert - the value should be the same reference (immutable UUID)
            assertThat(id.value()).isEqualTo(originalValue);
        }
    }
}
