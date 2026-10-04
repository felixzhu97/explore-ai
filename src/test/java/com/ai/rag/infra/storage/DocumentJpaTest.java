package com.ai.rag.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.model.DocumentStatus;
import com.ai.rag.domain.vo.DocumentId;
import com.ai.testsupport.AbstractDataJpaTest;
import com.ai.testsupport.JpaTestPackages;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = {"com.ai.rag.domain", JpaTestPackages.BASE, JpaTestPackages.COMMON})
@EnableJpaRepositories(basePackageClasses = SpringDataDocumentRepository.class)
@Import({DocumentRepository.class, OwnerPartitionScope.class})
class DocumentJpaTest extends AbstractDataJpaTest {

  private static final String OWNER_KEY = "c:33333333-3333-3333-3333-333333333333";
  private static final String OTHER_OWNER_KEY = OwnerKey.forClient("other").value();

  @Autowired private TestEntityManager em;
  @Autowired private SpringDataDocumentRepository repository;
  @Autowired private DocumentRepository adapter;

  @Test
  @DisplayName("should persist and reload document when round tripping")
  void shouldPersistAndReloadDocumentWhenRoundTripping() {
    Document document = new Document(DocumentId.generate(), "Guide", "guide.pdf", 2048L, OWNER_KEY);

    repository.saveAndFlush(document);
    em.clear();

    Optional<Document> reloaded = repository.findById(document.getId());

    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getTitle()).isEqualTo("Guide");
    assertThat(reloaded.get().getFileName()).isEqualTo("guide.pdf");
    assertThat(reloaded.get().getFileSize()).isEqualTo(2048L);
    assertThat(reloaded.get().getStatus()).isEqualTo(DocumentStatus.UPLOADING);
  }

  @Test
  @DisplayName("should store owner key when persisting partitioned document")
  void shouldStoreOwnerKeyWhenPersistingPartitionedDocument() {
    Document document =
        new Document(
            DocumentId.generate(),
            "Owned",
            "owned.pdf",
            512L,
            DocumentStatus.READY,
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-07-02T00:00:00Z"),
            OWNER_KEY);

    repository.saveAndFlush(document);
    em.clear();

    Document reloaded = repository.findById(document.getId()).orElseThrow();

    assertThat(reloaded.getClientId()).isEqualTo(OWNER_KEY);
  }

  @Test
  @DisplayName("should list documents by owner ordered by created at descending")
  void shouldListDocumentsByOwnerOrderedByCreatedAtDescending() {
    Document older =
        new Document(
            DocumentId.generate(),
            "Older",
            "older.pdf",
            100L,
            DocumentStatus.READY,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z"),
            OWNER_KEY);
    Document newer =
        new Document(
            DocumentId.generate(),
            "Newer",
            "newer.pdf",
            200L,
            DocumentStatus.READY,
            Instant.parse("2026-06-01T00:00:00Z"),
            Instant.parse("2026-06-02T00:00:00Z"),
            OWNER_KEY);
    repository.saveAndFlush(older);
    repository.saveAndFlush(newer);
    em.clear();

    List<Document> documents = adapter.findAllByOwnerKey(OWNER_KEY);

    assertThat(documents).extracting(Document::getTitle).containsExactly("Newer", "Older");
  }

  @Test
  @DisplayName("should find document by id and owner key when scoped lookup")
  void shouldFindDocumentByIdAndOwnerKeyWhenScopedLookup() {
    Document document =
        new Document(DocumentId.generate(), "Scoped", "scoped.pdf", 128L, OWNER_KEY);
    repository.saveAndFlush(document);
    em.clear();

    Optional<Document> found = adapter.findByIdAndOwnerKey(document.getId().asUuid(), OWNER_KEY);

    assertThat(found).isPresent();
    assertThat(found.get().getTitle()).isEqualTo("Scoped");
    assertThat(adapter.findByIdAndOwnerKey(document.getId().asUuid(), OTHER_OWNER_KEY)).isEmpty();
  }

  @Test
  @DisplayName("should delete only the owner's document when scoped delete")
  void shouldDeleteOnlyTheOwnersDocumentWhenScopedDelete() {
    Document document =
        new Document(DocumentId.generate(), "Scoped", "scoped.pdf", 128L, OWNER_KEY);
    repository.saveAndFlush(document);
    em.clear();

    adapter.deleteByIdAndOwnerKey(document.getId().asUuid(), OTHER_OWNER_KEY);
    em.flush();
    assertThat(repository.existsById(document.getId())).isTrue();

    adapter.deleteByIdAndOwnerKey(document.getId().asUuid(), OWNER_KEY);
    em.flush();
    assertThat(repository.existsById(document.getId())).isFalse();
  }
}
