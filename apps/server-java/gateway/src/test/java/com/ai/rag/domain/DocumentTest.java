package com.ai.rag.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Document Tests")
class DocumentTest {

    @Nested
    @DisplayName("create factory method")
    class CreateFactoryMethodTests {

        @Test
        @DisplayName("should create document with PENDING status")
        void shouldCreateDocumentWithPendingStatus() {
            DocumentTitle title = DocumentTitle.of("Test Document");
            Document document = Document.create(title, "text/plain", 1024L);

            assertThat(document.getStatus()).isEqualTo(DocumentStatus.PENDING);
        }

        @Test
        @DisplayName("should create document with generated id")
        void shouldCreateDocumentWithGeneratedId() {
            DocumentTitle title = DocumentTitle.of("Test Document");
            Document document = Document.create(title, "text/plain", 1024L);

            assertThat(document.getId()).isNotNull();
        }

        @Test
        @DisplayName("should create document with given metadata")
        void shouldCreateDocumentWithGivenMetadata() {
            DocumentTitle title = DocumentTitle.of("My Document");
            Document document = Document.create(title, "application/pdf", 2048L);

            assertThat(document.getTitle()).isEqualTo(title);
            assertThat(document.getContentType()).isEqualTo("application/pdf");
            assertThat(document.getSize()).isEqualTo(2048L);
        }

        @Test
        @DisplayName("should create document with empty chunks")
        void shouldCreateDocumentWithEmptyChunks() {
            DocumentTitle title = DocumentTitle.of("Test Document");
            Document document = Document.create(title, "text/plain", 1024L);

            assertThat(document.getChunks()).isEmpty();
            assertThat(document.getChunkCount()).isZero();
        }

        @Test
        @DisplayName("should create document with same createdAt and updatedAt")
        void shouldCreateDocumentWithSameCreatedAtAndUpdatedAt() {
            DocumentTitle title = DocumentTitle.of("Test Document");
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            Document document = Document.create(title, "text/plain", 1024L);

            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertThat(document.getCreatedAt()).isAfterOrEqualTo(before);
            assertThat(document.getCreatedAt()).isBeforeOrEqualTo(after);
            assertThat(document.getUpdatedAt()).isEqualTo(document.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("reconstitute factory method")
    class ReconstituteFactoryMethodTests {

        @Test
        @DisplayName("should reconstitute document with given status")
        void shouldReconstituteDocumentWithGivenStatus() {
            DocumentId id = DocumentId.generate();
            DocumentTitle title = DocumentTitle.of("Reconstituted Document");
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime updatedAt = LocalDateTime.now();

            Document document = Document.reconstitute(
                    id, title, "text/plain", 512L,
                    DocumentStatus.COMPLETED, createdAt, updatedAt,
                    List.of()
            );

            assertThat(document.getId()).isEqualTo(id);
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.COMPLETED);
            assertThat(document.getCreatedAt()).isEqualTo(createdAt);
            assertThat(document.getUpdatedAt()).isEqualTo(updatedAt);
        }

        @Test
        @DisplayName("should reconstitute document with chunks")
        void shouldReconstituteDocumentWithChunks() {
            DocumentId id = DocumentId.generate();
            DocumentTitle title = DocumentTitle.of("Document with Chunks");
            DocumentId docId = DocumentId.generate();
            Chunk chunk1 = Chunk.create("First chunk text", 0, docId);
            Chunk chunk2 = Chunk.create("Second chunk text", 1, docId);
            List<Chunk> chunks = List.of(chunk1, chunk2);

            Document document = Document.reconstitute(
                    id, title, "text/plain", 512L,
                    DocumentStatus.COMPLETED,
                    LocalDateTime.now(), LocalDateTime.now(),
                    chunks
            );

            assertThat(document.getChunks()).hasSize(2);
            assertThat(document.getChunkCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("canBeProcessed")
    class CanBeProcessedTests {

        @Test
        @DisplayName("should return true when status is PENDING")
        void shouldReturnTrueWhenStatusIsPending() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );

            assertThat(document.canBeProcessed()).isTrue();
        }

        @Test
        @DisplayName("should return false when status is INDEXING")
        void shouldReturnFalseWhenStatusIsIndexing() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );
            document.startProcessing();

            assertThat(document.canBeProcessed()).isFalse();
        }

        @Test
        @DisplayName("should return false when status is COMPLETED")
        void shouldReturnFalseWhenStatusIsCompleted() {
            Document document = createCompletedDocument();

            assertThat(document.canBeProcessed()).isFalse();
        }

        @Test
        @DisplayName("should return false when status is FAILED")
        void shouldReturnFalseWhenStatusIsFailed() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );
            document.fail("Test failure");

            assertThat(document.canBeProcessed()).isFalse();
        }
    }

    @Nested
    @DisplayName("startProcessing")
    class StartProcessingTests {

        @Test
        @DisplayName("should transition from PENDING to INDEXING")
        void shouldTransitionFromPendingToIndexing() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );

            document.startProcessing();

            assertThat(document.getStatus()).isEqualTo(DocumentStatus.INDEXING);
        }

        @Test
        @DisplayName("should throw exception when not in PENDING state")
        void shouldThrowExceptionWhenNotInPendingState() {
            Document document = createCompletedDocument();

            assertThatThrownBy(document::startProcessing)
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("COMPLETED")
                    .hasMessageContaining("INDEXING");
        }

        @Test
        @DisplayName("should allow processing again after being reset to PENDING")
        void shouldAllowProcessingAgainAfterReset() {
            DocumentId id = DocumentId.generate();
            Document document = Document.reconstitute(
                    id, DocumentTitle.of("Test"), "text/plain", 100L,
                    DocumentStatus.PENDING,
                    LocalDateTime.now(), LocalDateTime.now(),
                    List.of()
            );

            document.startProcessing();
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.INDEXING);
        }
    }

    @Nested
    @DisplayName("addChunks")
    class AddChunksTests {

        @Test
        @DisplayName("should add chunks when in INDEXING state")
        void shouldAddChunksWhenInIndexingState() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );
            document.startProcessing();

            DocumentId docId = document.getId();
            Chunk chunk = Chunk.create("New chunk content", 0, docId);
            document.addChunks(List.of(chunk));

            assertThat(document.getChunks()).hasSize(1);
            assertThat(document.getChunks().get(0).text()).isEqualTo("New chunk content");
        }

        @Test
        @DisplayName("should add multiple chunks at once")
        void shouldAddMultipleChunksAtOnce() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );
            document.startProcessing();

            DocumentId docId = document.getId();
            List<Chunk> chunks = List.of(
                    Chunk.create("Chunk 1", 0, docId),
                    Chunk.create("Chunk 2", 1, docId),
                    Chunk.create("Chunk 3", 2, docId)
            );
            document.addChunks(chunks);

            assertThat(document.getChunks()).hasSize(3);
            assertThat(document.getChunkCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should throw exception when not in INDEXING state")
        void shouldThrowExceptionWhenNotInIndexingState() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );

            DocumentId docId = document.getId();
            Chunk chunk = Chunk.create("New chunk", 0, docId);

            assertThatThrownBy(() -> document.addChunks(List.of(chunk)))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("not being processed");
        }
    }

    @Nested
    @DisplayName("completeProcessing")
    class CompleteProcessingTests {

        @Test
        @DisplayName("should transition from INDEXING to COMPLETED")
        void shouldTransitionFromIndexingToCompleted() {
            Document document = createDocumentWithChunks();

            document.completeProcessing();

            assertThat(document.getStatus()).isEqualTo(DocumentStatus.COMPLETED);
        }

        @Test
        @DisplayName("should throw exception when no chunks exist")
        void shouldThrowExceptionWhenNoChunksExist() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );
            document.startProcessing();

            assertThatThrownBy(document::completeProcessing)
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("without any chunks");
        }

        @Test
        @DisplayName("should throw exception when not in INDEXING state")
        void shouldThrowExceptionWhenNotInIndexingState() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );

            assertThatThrownBy(document::completeProcessing)
                    .isInstanceOf(InvalidStateTransitionException.class);
        }
    }

    @Nested
    @DisplayName("fail")
    class FailTests {

        @Test
        @DisplayName("should transition from PENDING to FAILED")
        void shouldTransitionFromPendingToFailed() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );

            document.fail("Processing failed");

            assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        }

        @Test
        @DisplayName("should transition from INDEXING to FAILED")
        void shouldTransitionFromIndexingToFailed() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );
            document.startProcessing();

            document.fail("Indexing failed");

            assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
        }

        @Test
        @DisplayName("should throw exception when already in terminal state")
        void shouldThrowExceptionWhenAlreadyInTerminalState() {
            Document document = createCompletedDocument();

            assertThatThrownBy(() -> document.fail("Should not fail"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("terminal state");
        }
    }

    @Nested
    @DisplayName("isCompleted and hasFailed")
    class StatusQueryTests {

        @Test
        @DisplayName("should return true for isCompleted when COMPLETED")
        void shouldReturnTrueForIsCompletedWhenCompleted() {
            Document document = createCompletedDocument();

            assertThat(document.isCompleted()).isTrue();
            assertThat(document.hasFailed()).isFalse();
        }

        @Test
        @DisplayName("should return true for hasFailed when FAILED")
        void shouldReturnTrueForHasFailedWhenFailed() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );
            document.fail("Test failure");

            assertThat(document.hasFailed()).isTrue();
            assertThat(document.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("should return false for both when PENDING")
        void shouldReturnFalseForBothWhenPending() {
            Document document = Document.create(
                    DocumentTitle.of("Test"), "text/plain", 100L
            );

            assertThat(document.isCompleted()).isFalse();
            assertThat(document.hasFailed()).isFalse();
        }
    }

    @Nested
    @DisplayName("getChunks immutability")
    class GetChunksImmutabilityTests {

        @Test
        @DisplayName("should return unmodifiable list")
        void shouldReturnUnmodifiableList() {
            Document document = createDocumentWithChunks();

            assertThatThrownBy(() -> document.getChunks().add(
                    Chunk.create("Should not add", 5, document.getId())
            )).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    private Document createCompletedDocument() {
        Document document = Document.create(
                DocumentTitle.of("Test"), "text/plain", 100L
        );
        document.startProcessing();

        DocumentId docId = document.getId();
        document.addChunks(List.of(
                Chunk.create("Content", 0, docId)
        ));
        document.completeProcessing();

        return document;
    }

    private Document createDocumentWithChunks() {
        Document document = Document.create(
                DocumentTitle.of("Test"), "text/plain", 100L
        );
        document.startProcessing();

        DocumentId docId = document.getId();
        document.addChunks(List.of(
                Chunk.create("Chunk 1", 0, docId),
                Chunk.create("Chunk 2", 1, docId)
        ));

        return document;
    }
}
