package com.ai.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Document Aggregate Root Tests
 * 
 * Tests for Document lifecycle and state transitions following TDD principles:
 * - Naming convention: should_expected_result_when_condition
 * - Uses AAA pattern (Arrange-Act-Assert)
 * - Tests business rules for status transitions
 */
@DisplayName("Document")
class DocumentTest {

    private static final UUID TEST_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final String TEST_TITLE = "Test Document";
    private static final String TEST_FILE_NAME = "test.pdf";
    private static final Long TEST_FILE_SIZE = 1024L;

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create document with UPLOADING status")
        void shouldCreateDocumentWithUploadingStatus() {
            // Act
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert
            assertThat(document.getStatus()).isEqualTo(Document.DocumentStatus.UPLOADING);
        }

        @Test
        @DisplayName("should create document with all provided fields")
        void shouldCreateDocumentWithAllProvidedFields() {
            // Act
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert
            assertThat(document.getId()).isEqualTo(TEST_ID);
            assertThat(document.getTitle()).isEqualTo(TEST_TITLE);
            assertThat(document.getFileName()).isEqualTo(TEST_FILE_NAME);
            assertThat(document.getFileSize()).isEqualTo(TEST_FILE_SIZE);
        }

        @Test
        @DisplayName("should initialize with current timestamp")
        void shouldInitializeWithCurrentTimestamp() {
            // Arrange
            Instant before = Instant.now();

            // Act
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert
            Instant after = Instant.now();
            assertThat(document.getCreatedAt()).isAfterOrEqualTo(before);
            assertThat(document.getCreatedAt()).isBeforeOrEqualTo(after);
            assertThat(document.getUpdatedAt()).isAfterOrEqualTo(before);
            assertThat(document.getUpdatedAt()).isBeforeOrEqualTo(after);
        }

        @Test
        @DisplayName("should allow null title")
        void shouldAllowNullTitle() {
            // Act
            Document document = new Document(TEST_ID, null, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert
            assertThat(document.getTitle()).isNull();
        }

        @Test
        @DisplayName("should allow null file name")
        void shouldAllowNullFileName() {
            // Act
            Document document = new Document(TEST_ID, TEST_TITLE, null, TEST_FILE_SIZE);

            // Assert
            assertThat(document.getFileName()).isNull();
        }

        @Test
        @DisplayName("should allow null file size")
        void shouldAllowNullFileSize() {
            // Act
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, null);

            // Assert
            assertThat(document.getFileSize()).isNull();
        }
    }

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitions {

        @Test
        @DisplayName("should transition from UPLOADING to PROCESSING when markProcessing()")
        void shouldTransitionFromUploadingToProcessingWhenMarkProcessing() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            assertThat(document.getStatus()).isEqualTo(Document.DocumentStatus.UPLOADING);

            // Act
            document.markProcessing();

            // Assert
            assertThat(document.getStatus()).isEqualTo(Document.DocumentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should transition from PROCESSING to READY when markReady()")
        void shouldTransitionFromProcessingToReadyWhenMarkReady() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();

            // Act
            document.markReady();

            // Assert
            assertThat(document.getStatus()).isEqualTo(Document.DocumentStatus.READY);
        }

        @Test
        @DisplayName("should transition from PROCESSING to FAILED when markFailed()")
        void shouldTransitionFromProcessingToFailedWhenMarkFailed() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();

            // Act
            document.markFailed();

            // Assert
            assertThat(document.getStatus()).isEqualTo(Document.DocumentStatus.FAILED);
        }

        @Test
        @DisplayName("should transition from READY to PROCESSING when markProcessing() (re-processing)")
        void shouldTransitionFromReadyToProcessingWhenMarkProcessing() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markReady();
            assertThat(document.getStatus()).isEqualTo(Document.DocumentStatus.READY);

            // Act
            document.markProcessing();

            // Assert
            assertThat(document.getStatus()).isEqualTo(Document.DocumentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should transition from FAILED to PROCESSING when markProcessing() (retry)")
        void shouldTransitionFromFailedToProcessingWhenMarkProcessing() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markFailed();
            assertThat(document.getStatus()).isEqualTo(Document.DocumentStatus.FAILED);

            // Act
            document.markProcessing();

            // Assert
            assertThat(document.getStatus()).isEqualTo(Document.DocumentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should update updatedAt timestamp on status change")
        void shouldUpdateUpdatedAtTimestampOnStatusChange() throws InterruptedException {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            Instant originalUpdatedAt = document.getUpdatedAt();
            Thread.sleep(10);

            // Act
            document.markProcessing();

            // Assert
            assertThat(document.getUpdatedAt()).isAfter(originalUpdatedAt);
        }
    }

    @Nested
    @DisplayName("isReady")
    class IsReady {

        @Test
        @DisplayName("should return false when status is UPLOADING")
        void shouldReturnFalseWhenStatusIsUploading() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert
            assertThat(document.isReady()).isFalse();
        }

        @Test
        @DisplayName("should return false when status is PROCESSING")
        void shouldReturnFalseWhenStatusIsProcessing() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();

            // Assert
            assertThat(document.isReady()).isFalse();
        }

        @Test
        @DisplayName("should return true when status is READY")
        void shouldReturnTrueWhenStatusIsReady() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markReady();

            // Assert
            assertThat(document.isReady()).isTrue();
        }

        @Test
        @DisplayName("should return false when status is FAILED")
        void shouldReturnFalseWhenStatusIsFailed() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markFailed();

            // Assert
            assertThat(document.isReady()).isFalse();
        }
    }

    @Nested
    @DisplayName("Document Status Enum")
    class DocumentStatusEnum {

        @Test
        @DisplayName("should have four status values")
        void shouldHaveFourStatusValues() {
            // Assert
            assertThat(Document.DocumentStatus.values()).hasSize(4);
        }

        @Test
        @DisplayName("should contain UPLOADING status")
        void shouldContainUploadingStatus() {
            // Assert
            assertThat(Document.DocumentStatus.UPLOADING).isNotNull();
        }

        @Test
        @DisplayName("should contain PROCESSING status")
        void shouldContainProcessingStatus() {
            // Assert
            assertThat(Document.DocumentStatus.PROCESSING).isNotNull();
        }

        @Test
        @DisplayName("should contain READY status")
        void shouldContainReadyStatus() {
            // Assert
            assertThat(Document.DocumentStatus.READY).isNotNull();
        }

        @Test
        @DisplayName("should contain FAILED status")
        void shouldContainFailedStatus() {
            // Assert
            assertThat(Document.DocumentStatus.FAILED).isNotNull();
        }
    }
}
