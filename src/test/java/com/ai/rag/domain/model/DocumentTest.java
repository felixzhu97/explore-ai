package com.ai.rag.domain.model;

import com.ai.rag.domain.model.DocumentStatus;
import com.ai.rag.domain.vo.DocumentId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        void shouldCreateWithUploadingStatus() {
            Document doc = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.UPLOADING);
        }

        @Test
        @DisplayName("should initialize timestamps")
        void shouldInitializeTimestamps() {
            Instant before = Instant.now();
            Document doc = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            assertThat(doc.getCreatedAt()).isAfterOrEqualTo(before);
            assertThat(doc.getUpdatedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("should allow null title")
        void shouldAllowNullTitle() {
            Document doc = new Document(TEST_ID, null, TEST_FILE_NAME, TEST_FILE_SIZE);
            assertThat(doc.getTitle()).isNull();
        }

        @Test
        @DisplayName("should truncate long title")
        void shouldTruncateLongTitle() {
            String longTitle = "A".repeat(300);
            Document doc = new Document(TEST_ID, longTitle, TEST_FILE_NAME, TEST_FILE_SIZE);
            assertThat(doc.getTitle()).hasSize(255);
        }

        @Test
        @DisplayName("should trim title whitespace")
        void shouldTrimTitle() {
            Document doc = new Document(TEST_ID, "  Test  ", TEST_FILE_NAME, TEST_FILE_SIZE);
            assertThat(doc.getTitle()).isEqualTo("Test");
        }
    }

    @Nested
    @DisplayName("Status Transitions")
    class StatusTransitions {

        @Test
        @DisplayName("should transition UPLOADING -> PROCESSING")
        void shouldTransitionUploadingToProcessing() {
            Document doc = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.UPLOADING);
            doc.markProcessing();
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should transition PROCESSING -> READY")
        void shouldTransitionProcessingToReady() {
            Document doc = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            doc.markProcessing();
            doc.markReady();
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.READY);
        }

        @Test
        @DisplayName("should transition PROCESSING -> FAILED")
        void shouldTransitionProcessingToFailed() {
            Document doc = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            doc.markProcessing();
            doc.markFailed();
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
        }

        @Test
        @DisplayName("should transition FAILED -> PROCESSING")
        void shouldTransitionFailedToProcessing() {
            Document doc = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            doc.markProcessing();
            doc.markFailed();
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
            doc.markProcessing();
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        }

        @Test
        @DisplayName("should not allow READY -> FAILED transition")
        void shouldNotAllowReadyToFailed() {
            Document doc = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            doc.markProcessing();
            doc.markReady();
            assertThatThrownBy(doc::markFailed)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        @DisplayName("should update updatedAt on status change")
        void shouldUpdateUpdatedAt() throws InterruptedException {
            Document doc = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            Instant original = doc.getUpdatedAt();
            Thread.sleep(10);
            doc.markProcessing();
            assertThat(doc.getUpdatedAt()).isAfter(original);
        }
    }

    @Nested
    @DisplayName("updateTitle")
    class UpdateTitle {

        @Test
        @DisplayName("should update title when not READY")
        void shouldUpdateTitleWhenNotReady() {
            Document doc = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            doc.updateTitle("New Title");
            assertThat(doc.getTitle()).isEqualTo("New Title");
        }

        @Test
        @DisplayName("should throw when updating READY document")
        void shouldThrowWhenUpdatingReadyDocument() {
            Document doc = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            doc.markProcessing();
            doc.markReady();
            assertThatThrownBy(() -> doc.updateTitle("New Title"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Equals & HashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal when same ID")
        void shouldEqualWhenSameId() {
            Document doc1 = new Document(TEST_ID, "A", "a.pdf", 100L);
            Document doc2 = new Document(TEST_ID, "B", "b.pdf", 200L);
            assertThat(doc1).isEqualTo(doc2);
            assertThat(doc1.hashCode()).isEqualTo(doc2.hashCode());
        }

        @Test
        @DisplayName("should not equal different ID")
        void shouldNotEqualDifferentId() {
            DocumentId other = DocumentId.of(UUID.randomUUID());
            Document doc1 = new Document(TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            Document doc2 = new Document(other, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE);
            assertThat(doc1).isNotEqualTo(doc2);
        }
    }

    @Nested
    @DisplayName("Full Constructor")
    class FullConstructor {

        @Test
        @DisplayName("should create with all fields")
        void shouldCreateWithAllFields() {
            Instant created = Instant.now().minusSeconds(3600);
            Instant updated = Instant.now().minusSeconds(1800);
            Document doc = new Document(
                    TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE,
                    DocumentStatus.READY, created, updated);
            assertThat(doc.getId()).isEqualTo(TEST_ID);
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.READY);
            assertThat(doc.getCreatedAt()).isEqualTo(created);
            assertThat(doc.getUpdatedAt()).isEqualTo(updated);
        }

        @Test
        @DisplayName("should allow restoring to FAILED")
        void shouldAllowRestoringToFailed() {
            Instant created = Instant.now().minusSeconds(3600);
            Instant updated = Instant.now().minusSeconds(1800);
            Document doc = new Document(
                    TEST_ID, TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE,
                    DocumentStatus.FAILED, created, updated);
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
        }
    }
}
