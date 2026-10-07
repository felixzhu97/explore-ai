package com.ai.rag.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ai.common.domain.model.OwnerKey;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RagDocument")
class RagDocumentTest {

  private static final DocumentId TEST_ID =
      DocumentId.of(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
  private static final String TEST_TITLE = "Test Document";
  private static final String TEST_FILE_NAME = "test.pdf";
  private static final long TEST_FILE_SIZE = 1024L;
  private static final String TEST_OWNER_KEY = "c:test";

  private static RagDocument processing() {
    return RagDocument.startIngestion(TEST_TITLE, TEST_FILE_NAME, TEST_FILE_SIZE, TEST_OWNER_KEY);
  }

  @Nested
  @DisplayName("startIngestion()")
  class StartIngestion {

    @Test
    @DisplayName("should start processing with no chunks when a file is uploaded")
    void shouldStartProcessingWithNoChunksWhenAFileIsUploaded() {
      Instant before = Instant.now();

      RagDocument doc = processing();

      assertThat(doc.getStatus()).isEqualTo(DocumentStatus.PROCESSING);
      assertThat(doc.getChunkCount()).isZero();
      assertThat(doc.getFileSize()).isEqualTo(TEST_FILE_SIZE);
      assertThat(doc.getCreatedAt()).isAfterOrEqualTo(before);
      assertThat(doc.isSearchable()).isFalse();
    }

    @Test
    @DisplayName("should fall back to the file name when the title is blank")
    void shouldFallBackToTheFileNameWhenTheTitleIsBlank() {
      RagDocument doc = RagDocument.startIngestion("  ", "guide.txt", 10, TEST_OWNER_KEY);

      assertThat(doc.getTitle()).isEqualTo("guide.txt");
    }

    @Test
    @DisplayName("should fall back to Untitled when there is no title or file name")
    void shouldFallBackToUntitledWhenThereIsNoTitleOrFileName() {
      RagDocument doc = RagDocument.startIngestion(null, null, 10, TEST_OWNER_KEY);

      assertThat(doc.getTitle()).isEqualTo(RagDocument.UNTITLED);
    }

    @Test
    @DisplayName("should trim and cap the title when it is long")
    void shouldTrimAndCapTheTitleWhenItIsLong() {
      RagDocument trimmed = RagDocument.startIngestion("  Test  ", null, 10, TEST_OWNER_KEY);
      RagDocument capped =
          RagDocument.startIngestion(" " + "A".repeat(300), null, 10, TEST_OWNER_KEY);

      assertThat(trimmed.getTitle()).isEqualTo("Test");
      assertThat(capped.getTitle()).hasSize(RagDocument.MAX_TITLE_LENGTH).startsWith("A");
    }

    @Test
    @DisplayName("should reject an empty file")
    void shouldRejectAnEmptyFile() {
      assertThatThrownBy(() -> RagDocument.startIngestion("Empty", "e.txt", 0, TEST_OWNER_KEY))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Uploaded file is empty");
    }
  }

  @Nested
  @DisplayName("completeIngestion()")
  class CompleteIngestion {

    @Test
    @DisplayName("should become searchable with its chunk count when ingestion completes")
    void shouldBecomeSearchableWithItsChunkCountWhenIngestionCompletes() {
      RagDocument doc = processing();

      doc.completeIngestion(3);

      assertThat(doc.getStatus()).isEqualTo(DocumentStatus.READY);
      assertThat(doc.getChunkCount()).isEqualTo(3);
      assertThat(doc.isSearchable()).isTrue();
    }

    @Test
    @DisplayName("should reject completing without chunks")
    void shouldRejectCompletingWithoutChunks() {
      RagDocument doc = processing();

      assertThatThrownBy(() -> doc.completeIngestion(0))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should reject completing twice")
    void shouldRejectCompletingTwice() {
      RagDocument doc = processing();
      doc.completeIngestion(1);

      assertThatThrownBy(() -> doc.completeIngestion(1)).isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("failIngestion()")
  class FailIngestion {

    @Test
    @DisplayName("should fail without chunks when ingestion breaks")
    void shouldFailWithoutChunksWhenIngestionBreaks() {
      RagDocument doc = processing();

      doc.failIngestion();

      assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
      assertThat(doc.getChunkCount()).isZero();
      assertThat(doc.isSearchable()).isFalse();
    }

    @Test
    @DisplayName("should fail a document marked ready in memory when its save rolls back")
    void shouldFailADocumentMarkedReadyInMemoryWhenItsSaveRollsBack() {
      RagDocument doc = processing();
      doc.completeIngestion(2);

      doc.failIngestion();

      assertThat(doc.getStatus()).isEqualTo(DocumentStatus.FAILED);
      assertThat(doc.getChunkCount()).isZero();
    }

    @Test
    @DisplayName("should keep the first failure time when failed again")
    void shouldKeepTheFirstFailureTimeWhenFailedAgain() {
      RagDocument doc = processing();
      doc.failIngestion();
      Instant failedAt = doc.getUpdatedAt();

      doc.failIngestion();

      assertThat(doc.getUpdatedAt()).isEqualTo(failedAt);
    }
  }

  @Nested
  @DisplayName("newChunk()")
  class NewChunk {

    @Test
    @DisplayName("should tag the chunk with the document owner title and file name")
    void shouldTagTheChunkWithTheDocumentOwnerTitleAndFileName() {
      RagDocument doc = processing();

      DocumentChunk chunk = doc.newChunk(2, "text", Map.of("page", 4));

      assertThat(chunk.getDocumentId()).isEqualTo(doc.getId());
      assertThat(chunk.getOwnerKey()).isEqualTo(OwnerKey.parse(TEST_OWNER_KEY));
      assertThat(chunk.getChunkIndex()).isEqualTo(2);
      assertThat(chunk.getMetadata())
          .containsEntry("page", 4)
          .containsEntry(ChunkMetadataKeys.TITLE, TEST_TITLE)
          .containsEntry(ChunkMetadataKeys.FILE_NAME, TEST_FILE_NAME)
          .doesNotContainKey(ChunkMetadataKeys.OWNER_KEY);
    }

    @Test
    @DisplayName("should leave out the file name when the document has none")
    void shouldLeaveOutTheFileNameWhenTheDocumentHasNone() {
      RagDocument doc = RagDocument.startIngestion("Notes", null, 5, TEST_OWNER_KEY);

      DocumentChunk chunk = doc.newChunk(0, "text", Map.of());

      assertThat(chunk.getMetadata()).doesNotContainKey(ChunkMetadataKeys.FILE_NAME);
    }
  }

  @Nested
  @DisplayName("updateTitle()")
  class UpdateTitle {

    @Test
    @DisplayName("should update title when not ready")
    void shouldUpdateTitleWhenNotReady() {
      RagDocument doc = processing();
      doc.updateTitle("New Title");
      assertThat(doc.getTitle()).isEqualTo("New Title");
    }

    @Test
    @DisplayName("should throw when updating a ready document")
    void shouldThrowWhenUpdatingAReadyDocument() {
      RagDocument doc = processing();
      doc.completeIngestion(1);
      assertThatThrownBy(() -> doc.updateTitle("New Title"))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("restore()")
  class Restore {

    @Test
    @DisplayName("should restore every stored field")
    void shouldRestoreEveryStoredField() {
      Instant created = Instant.now().minusSeconds(3600);
      Instant updated = Instant.now().minusSeconds(1800);

      RagDocument doc =
          RagDocument.restore(
              TEST_ID,
              TEST_TITLE,
              TEST_FILE_NAME,
              TEST_FILE_SIZE,
              DocumentStatus.READY,
              7,
              created,
              updated,
              TEST_OWNER_KEY);

      assertThat(doc.getId()).isEqualTo(TEST_ID);
      assertThat(doc.getStatus()).isEqualTo(DocumentStatus.READY);
      assertThat(doc.getChunkCount()).isEqualTo(7);
      assertThat(doc.getCreatedAt()).isEqualTo(created);
      assertThat(doc.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    @DisplayName("should be equal when ids match")
    void shouldBeEqualWhenIdsMatch() {
      Instant now = Instant.now();
      RagDocument first =
          RagDocument.restore(
              TEST_ID, "A", "a.pdf", 1L, DocumentStatus.READY, 1, now, now, TEST_OWNER_KEY);
      RagDocument second =
          RagDocument.restore(
              TEST_ID, "B", "b.pdf", 2L, DocumentStatus.FAILED, 0, now, now, TEST_OWNER_KEY);

      assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
      assertThat(processing()).isNotEqualTo(first);
    }

    @Test
    @DisplayName("should not print the title")
    void shouldNotPrintTheTitle() {
      assertThat(processing().toString()).doesNotContain(TEST_TITLE);
    }
  }
}
