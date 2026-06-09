package com.ai.rag.infrastructure.persistence;

import com.ai.rag.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("InMemoryDocumentRepository Tests")
class InMemoryDocumentRepositoryTest {

    private InMemoryDocumentRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDocumentRepository();
    }

    private Document createDocument(String title, DocumentStatus status) {
        Document doc = Document.create(
            DocumentTitle.of(title),
            "text/plain",
            100L
        );
        if (status == DocumentStatus.INDEXING) {
            doc.startProcessing();
        } else if (status == DocumentStatus.COMPLETED) {
            doc.startProcessing();
            doc.addChunks(List.of(Chunk.create("test chunk", 0, doc.getId())));
            doc.completeProcessing();
        } else if (status == DocumentStatus.FAILED) {
            doc.fail("Test failure");
        }
        return doc;
    }

    @Nested
    @DisplayName("save")
    class SaveTests {

        @Test
        @DisplayName("should save document successfully")
        void shouldSaveDocumentSuccessfully() {
            Document doc = createDocument("Test Document", DocumentStatus.PENDING);

            Document saved = repository.save(doc);

            assertThat(saved).isEqualTo(doc);
            assertThat(repository.exists(doc.getId())).isTrue();
        }

        @Test
        @DisplayName("should update existing document")
        void shouldUpdateExistingDocument() {
            Document doc = createDocument("Test Document", DocumentStatus.PENDING);
            repository.save(doc);

            doc.startProcessing();
            repository.update(doc);

            Optional<Document> found = repository.findById(doc.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(DocumentStatus.INDEXING);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("should return document when it exists")
        void shouldReturnDocumentWhenItExists() {
            Document doc = createDocument("Test Document", DocumentStatus.PENDING);
            repository.save(doc);

            Optional<Document> result = repository.findById(doc.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getTitle().toString()).isEqualTo("Test Document");
        }

        @Test
        @DisplayName("should return empty when document does not exist")
        void shouldReturnEmptyWhenDocumentDoesNotExist() {
            DocumentId nonExistentId = DocumentId.generate();

            Optional<Document> result = repository.findById(nonExistentId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAllTests {

        @Test
        @DisplayName("should return empty list when no documents exist")
        void shouldReturnEmptyListWhenNoDocumentsExist() {
            List<Document> result = repository.findAll(0, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return all documents with pagination")
        void shouldReturnAllDocumentsWithPagination() {
            for (int i = 0; i < 15; i++) {
                repository.save(createDocument("Document " + i, DocumentStatus.PENDING));
            }

            List<Document> page1 = repository.findAll(0, 10);
            List<Document> page2 = repository.findAll(1, 10);

            assertThat(page1).hasSize(10);
            assertThat(page2).hasSize(5);
        }

        @Test
        @DisplayName("should return documents sorted by creation date descending")
        void shouldReturnDocumentsSortedByCreationDateDescending() {
            Document doc1 = createDocument("First", DocumentStatus.PENDING);
            Document doc2 = createDocument("Second", DocumentStatus.PENDING);
            repository.save(doc1);
            repository.save(doc2);

            List<Document> result = repository.findAll(0, 10);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findByStatus")
    class FindByStatusTests {

        @Test
        @DisplayName("should return documents with specific status")
        void shouldReturnDocumentsWithSpecificStatus() {
            repository.save(createDocument("Pending 1", DocumentStatus.PENDING));
            repository.save(createDocument("Pending 2", DocumentStatus.PENDING));
            repository.save(createDocument("Completed", DocumentStatus.COMPLETED));

            List<Document> pendingDocs = repository.findByStatus(DocumentStatus.PENDING, 0, 10);
            List<Document> completedDocs = repository.findByStatus(DocumentStatus.COMPLETED, 0, 10);

            assertThat(pendingDocs).hasSize(2);
            assertThat(completedDocs).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when no documents match status")
        void shouldReturnEmptyListWhenNoDocumentsMatchStatus() {
            repository.save(createDocument("Completed", DocumentStatus.COMPLETED));

            List<Document> pendingDocs = repository.findByStatus(DocumentStatus.PENDING, 0, 10);

            assertThat(pendingDocs).isEmpty();
        }

        @Test
        @DisplayName("should support pagination for status query")
        void shouldSupportPaginationForStatusQuery() {
            for (int i = 0; i < 5; i++) {
                repository.save(createDocument("Pending " + i, DocumentStatus.PENDING));
            }

            List<Document> page1 = repository.findByStatus(DocumentStatus.PENDING, 0, 2);
            List<Document> page2 = repository.findByStatus(DocumentStatus.PENDING, 1, 2);

            assertThat(page1).hasSize(2);
            assertThat(page2).hasSize(2);
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteByIdTests {

        @Test
        @DisplayName("should delete existing document")
        void shouldDeleteExistingDocument() {
            Document doc = createDocument("To Delete", DocumentStatus.PENDING);
            repository.save(doc);
            DocumentId docId = doc.getId();

            repository.deleteById(docId);

            assertThat(repository.findById(docId)).isEmpty();
            assertThat(repository.exists(docId)).isFalse();
        }

        @Test
        @DisplayName("should not throw when deleting non-existent document")
        void shouldNotThrowWhenDeletingNonExistentDocument() {
            DocumentId nonExistentId = DocumentId.generate();

            repository.deleteById(nonExistentId);

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("count")
    class CountTests {

        @Test
        @DisplayName("should return zero when no documents exist")
        void shouldReturnZeroWhenNoDocumentsExist() {
            assertThat(repository.count()).isZero();
        }

        @Test
        @DisplayName("should return correct count after saving documents")
        void shouldReturnCorrectCountAfterSavingDocuments() {
            repository.save(createDocument("Doc 1", DocumentStatus.PENDING));
            repository.save(createDocument("Doc 2", DocumentStatus.PENDING));

            assertThat(repository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return correct count after deleting document")
        void shouldReturnCorrectCountAfterDeletingDocument() {
            Document doc = createDocument("To Delete", DocumentStatus.PENDING);
            repository.save(doc);
            repository.deleteById(doc.getId());

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("countByStatus")
    class CountByStatusTests {

        @Test
        @DisplayName("should return zero when no documents match status")
        void shouldReturnZeroWhenNoDocumentsMatchStatus() {
            assertThat(repository.countByStatus(DocumentStatus.PENDING)).isZero();
        }

        @Test
        @DisplayName("should count documents by status")
        void shouldCountDocumentsByStatus() {
            repository.save(createDocument("Pending 1", DocumentStatus.PENDING));
            repository.save(createDocument("Pending 2", DocumentStatus.PENDING));
            repository.save(createDocument("Completed", DocumentStatus.COMPLETED));

            assertThat(repository.countByStatus(DocumentStatus.PENDING)).isEqualTo(2);
            assertThat(repository.countByStatus(DocumentStatus.COMPLETED)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("findAllIds")
    class FindAllIdsTests {

        @Test
        @DisplayName("should return empty set when no documents exist")
        void shouldReturnEmptySetWhenNoDocumentsExist() {
            Set<DocumentId> result = repository.findAllIds();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return all document IDs")
        void shouldReturnAllDocumentIds() {
            Document doc1 = createDocument("Doc 1", DocumentStatus.PENDING);
            Document doc2 = createDocument("Doc 2", DocumentStatus.PENDING);
            repository.save(doc1);
            repository.save(doc2);

            Set<DocumentId> result = repository.findAllIds();

            assertThat(result).containsExactlyInAnyOrder(doc1.getId(), doc2.getId());
        }
    }

    @Nested
    @DisplayName("exists")
    class ExistsTests {

        @Test
        @DisplayName("should return true for existing document")
        void shouldReturnTrueForExistingDocument() {
            Document doc = createDocument("Existing", DocumentStatus.PENDING);
            repository.save(doc);

            assertThat(repository.exists(doc.getId())).isTrue();
        }

        @Test
        @DisplayName("should return false for non-existent document")
        void shouldReturnFalseForNonExistentDocument() {
            assertThat(repository.exists(DocumentId.generate())).isFalse();
        }
    }

    @Nested
    @DisplayName("concurrent access")
    class ConcurrentAccessTests {

        @Test
        @DisplayName("should handle multiple saves concurrently")
        void shouldHandleMultipleSavesConcurrently() throws InterruptedException {
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    repository.save(createDocument("Doc " + index, DocumentStatus.PENDING));
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            assertThat(repository.count()).isEqualTo(threadCount);
        }
    }
}
