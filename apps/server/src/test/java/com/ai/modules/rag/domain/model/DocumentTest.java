package com.ai.modules.rag.domain.model;

import com.ai.modules.rag.domain.model.Document;
import com.ai.modules.rag.domain.model.DocumentStatus;
import com.ai.modules.rag.domain.vo.DocumentId;
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

    private static final DocumentId TEST_ID = DocumentId.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
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
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOADING);
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
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.UPLOADING);

            // Act
            document.markProcessing();

            // Assert
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
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
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.READY);
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
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        }

        @Test
        @DisplayName("should transition from READY to PROCESSING when markProcessing() (re-processing)")
        void shouldTransitionFromReadyToProcessingWhenMarkProcessing() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markReady();
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.READY);

            // Act
            document.markProcessing();

            // Assert
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should transition from FAILED to PROCESSING when markProcessing() (retry)")
        void shouldTransitionFromFailedToProcessingWhenMarkProcessing() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markFailed();
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);

            // Act
            document.markProcessing();

            // Assert
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
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
            assertThat(DocumentStatus.values()).hasSize(4);
        }

        @Test
        @DisplayName("should contain UPLOADING status")
        void shouldContainUploadingStatus() {
            // Assert
            assertThat(DocumentStatus.UPLOADING).isNotNull();
        }

        @Test
        @DisplayName("should contain PROCESSING status")
        void shouldContainProcessingStatus() {
            // Assert
            assertThat(DocumentStatus.PROCESSING).isNotNull();
        }

        @Test
        @DisplayName("should contain READY status")
        void shouldContainReadyStatus() {
            // Assert
            assertThat(DocumentStatus.READY).isNotNull();
        }

        @Test
        @DisplayName("should contain FAILED status")
        void shouldContainFailedStatus() {
            // Assert
            assertThat(DocumentStatus.FAILED).isNotNull();
        }
    }

    @Nested
    @DisplayName("prepareForRetry")
    class PrepareForRetry {

        @Test
        @DisplayName("should retry document when in FAILED status")
        void shouldRetryDocumentWhenInFailedStatus() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markFailed();
            assertThat(document.isFailed()).isTrue();

            // Act
            document.prepareForRetry();

            // Assert
            assertThat(document.isUploading()).isTrue();
            assertThat(document.isFailed()).isFalse();
        }

        @Test
        @DisplayName("should throw exception when retry in non-FAILED status")
        void shouldThrowExceptionWhenRetryInNonFailedStatus() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Act & Assert
            assertThatThrownBy(document::prepareForRetry)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot retry document in status: UPLOADING");
        }

        @Test
        @DisplayName("should increment retry count")
        void shouldIncrementRetryCount() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markFailed();

            // Act
            document.prepareForRetry();

            // Assert
            assertThat(document.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should increment retry count multiple times")
        void shouldIncrementRetryCountMultipleTimes() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markFailed();

            // Act
            document.prepareForRetry();
            document.markProcessing();
            document.markFailed();
            document.prepareForRetry();

            // Assert
            assertThat(document.getRetryCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("updateTitle")
    class UpdateTitle {

        @Test
        @DisplayName("should update title when document is not ready")
        void shouldUpdateTitleWhenDocumentIsNotReady() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Act
            document.updateTitle("New Title");

            // Assert
            assertThat(document.getTitle()).isEqualTo("New Title");
        }

        @Test
        @DisplayName("should update title when document is processing")
        void shouldUpdateTitleWhenDocumentIsProcessing() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();

            // Act
            document.updateTitle("Processing Title");

            // Assert
            assertThat(document.getTitle()).isEqualTo("Processing Title");
        }

        @Test
        @DisplayName("should throw exception when update ready document")
        void shouldThrowExceptionWhenUpdateReadyDocument() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markReady();

            // Act & Assert
            assertThatThrownBy(() -> document.updateTitle("New Title"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot update title of ready document");
        }

        @Test
        @DisplayName("should throw exception when update FAILED document")
        void shouldThrowExceptionWhenUpdateFailedDocument() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markFailed();

            // Act & Assert - FAILED document should still be updatable (not ready)
            document.updateTitle("Updated After Failure");
            assertThat(document.getTitle()).isEqualTo("Updated After Failure");
        }
    }

    @Nested
    @DisplayName("query methods")
    class QueryMethods {

        @Test
        @DisplayName("should return true for isFailed when status is FAILED")
        void shouldReturnTrueForIsFailed() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markFailed();

            // Assert
            assertThat(document.isFailed()).isTrue();
        }

        @Test
        @DisplayName("should return false for isFailed when status is not FAILED")
        void shouldReturnFalseForIsFailedWhenNotFailed() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert
            assertThat(document.isFailed()).isFalse();
        }

        @Test
        @DisplayName("should return true for isProcessing when status is PROCESSING")
        void shouldReturnTrueForIsProcessing() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();

            // Assert
            assertThat(document.isProcessing()).isTrue();
        }

        @Test
        @DisplayName("should return false for isProcessing when status is not PROCESSING")
        void shouldReturnFalseForIsProcessingWhenNotProcessing() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert
            assertThat(document.isProcessing()).isFalse();
        }

        @Test
        @DisplayName("should return true for isUploading when status is UPLOADING")
        void shouldReturnTrueForIsUploading() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert
            assertThat(document.isUploading()).isTrue();
        }

        @Test
        @DisplayName("should return false for isUploading when status is not UPLOADING")
        void shouldReturnFalseForIsUploadingWhenNotUploading() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();

            // Assert
            assertThat(document.isUploading()).isFalse();
        }

        @Test
        @DisplayName("should return retry count")
        void shouldReturnRetryCount() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            assertThat(document.getRetryCount()).isZero();

            // Act
            document.markProcessing();
            document.markFailed();
            document.prepareForRetry();

            // Assert
            assertThat(document.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return processing duration")
        void shouldReturnProcessingDuration() throws InterruptedException {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            Thread.sleep(10);

            // Act
            var duration = document.getProcessingDuration();

            // Assert
            assertThat(duration).isNotNull();
            assertThat(duration.toMillis()).isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("should return time since last update")
        void shouldReturnTimeSinceLastUpdate() throws InterruptedException {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            Instant beforeUpdate = document.getUpdatedAt();
            Thread.sleep(10);

            // Act
            var timeSinceUpdate = document.getTimeSinceLastUpdate();

            // Assert
            assertThat(timeSinceUpdate).isNotNull();
            assertThat(timeSinceUpdate.toMillis()).isGreaterThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("canTransitionTo")
    class CanTransitionTo {

        @Test
        @DisplayName("should return true for valid transitions")
        void shouldReturnTrueForValidTransitions() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert - UPLOADING can transition to PROCESSING
            assertThat(document.canTransitionTo(DocumentStatus.PROCESSING)).isTrue();

            // UPLOADING cannot transition to READY
            assertThat(document.canTransitionTo(DocumentStatus.READY)).isFalse();

            // UPLOADING cannot transition to UPLOADING itself
            assertThat(document.canTransitionTo(DocumentStatus.UPLOADING)).isFalse();
        }

        @Test
        @DisplayName("should return false for invalid transitions")
        void shouldReturnFalseForInvalidTransitions() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Cannot go back to UPLOADING
            assertThat(document.canTransitionTo(DocumentStatus.UPLOADING)).isFalse();

            // Cannot self-transition
            assertThat(document.canTransitionTo(DocumentStatus.PROCESSING)).isTrue();
        }

        @Test
        @DisplayName("should allow PROCESSING from non-PROCESSING states")
        void shouldAllowProcessingFromNonProcessingStates() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markFailed();

            // Act
            boolean canTransition = document.canTransitionTo(DocumentStatus.PROCESSING);

            // Assert
            assertThat(canTransition).isTrue();
        }

        @Test
        @DisplayName("should allow FAILED from non-READY states")
        void shouldAllowFailedFromNonReadyStates() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Act
            boolean canTransition = document.canTransitionTo(DocumentStatus.FAILED);

            // Assert
            assertThat(canTransition).isTrue();
        }

        @Test
        @DisplayName("should not allow FAILED from READY state")
        void shouldNotAllowFailedFromReadyState() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            document.markProcessing();
            document.markReady();

            // Act
            boolean canTransition = document.canTransitionTo(DocumentStatus.FAILED);

            // Assert
            assertThat(canTransition).isFalse();
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should equal when same ID")
        void shouldEqualWhenSameId() {
            // Arrange
            Document doc1 = new Document(TEST_ID, "Title 1", "file1.pdf", 1000L);
            Document doc2 = new Document(TEST_ID, "Title 2", "file2.pdf", 2000L);

            // Assert
            assertThat(doc1).isEqualTo(doc2);
            assertThat(doc1.hashCode()).isEqualTo(doc2.hashCode());
        }

        @Test
        @DisplayName("should not equal when different ID")
        void shouldNotEqualWhenDifferentId() {
            // Arrange
            DocumentId otherId = DocumentId.of(java.util.UUID.randomUUID());
            Document doc1 = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            Document doc2 = new Document(otherId, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert
            assertThat(doc1).isNotEqualTo(doc2);
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void shouldHaveConsistentHashCode() {
            // Arrange
            Document doc = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Act & Assert
            int hash1 = doc.hashCode();
            int hash2 = doc.hashCode();

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("should not equal to null")
        void shouldNotEqualToNull() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert
            assertThat(document).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not equal to different type")
        void shouldNotEqualToDifferentType() {
            // Arrange
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);

            // Assert
            assertThat(document).isNotEqualTo("Not a document");
        }
    }

    @Nested
    @DisplayName("full constructor")
    class FullConstructor {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            // Arrange
            DocumentId id = DocumentId.generate();
            String title = "Restored Document";
            String fileName = "restored.pdf";
            Long fileSize = 2048L;
            DocumentStatus status = DocumentStatus.PROCESSING;
            Instant createdAt = Instant.now().minusSeconds(3600);
            Instant updatedAt = Instant.now().minusSeconds(1800);

            // Act
            Document document = new Document(id, title, fileName, fileSize, status, createdAt, updatedAt);

            // Assert
            assertThat(document.getId()).isEqualTo(id);
            assertThat(document.getTitle()).isEqualTo(title);
            assertThat(document.getFileName()).isEqualTo(fileName);
            assertThat(document.getFileSize()).isEqualTo(fileSize);
            assertThat(document.getStatus()).isEqualTo(status);
            assertThat(document.getCreatedAt()).isEqualTo(createdAt);
            assertThat(document.getUpdatedAt()).isEqualTo(updatedAt);
            assertThat(document.getRetryCount()).isZero();
        }

        @Test
        @DisplayName("should allow restoring document to FAILED status")
        void shouldAllowRestoringDocumentToFailedStatus() {
            // Arrange
            Instant createdAt = Instant.now().minusSeconds(3600);
            Instant updatedAt = Instant.now().minusSeconds(1800);

            // Act
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE,
                    DocumentStatus.FAILED, createdAt, updatedAt);

            // Assert
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
            assertThat(document.isFailed()).isTrue();
        }

        @Test
        @DisplayName("should allow restoring document to READY status")
        void shouldAllowRestoringDocumentToReadyStatus() {
            // Arrange
            Instant createdAt = Instant.now().minusSeconds(3600);
            Instant updatedAt = Instant.now().minusSeconds(1800);

            // Act
            Document document = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE,
                    DocumentStatus.READY, createdAt, updatedAt);

            // Assert
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.READY);
            assertThat(document.isReady()).isTrue();
        }
    }
}
