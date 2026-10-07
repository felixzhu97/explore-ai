package com.ai.rag.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ai.common.domain.model.OwnerKey;
import com.ai.rag.domain.model.DocumentStatus;
import com.ai.rag.domain.model.RagDocument;
import com.ai.testsupport.AbstractDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DocumentRepositoryTest extends AbstractDataJpaTest {

  private static final OwnerKey OWNER = OwnerKey.parseKey("c:33333333-3333-3333-3333-333333333333");
  private static final OwnerKey OTHER = OwnerKey.createClientKey("other");

  @Autowired private DocumentRepository repository;

  @Test
  @DisplayName("should reload every field of the document only for its owner")
  void shouldReloadEveryFieldOfTheDocumentOnlyForItsOwner() {
    RagDocument document = save("Guide", OWNER);
    flushAndClear();

    RagDocument reloaded = repository.findByIdAndOwnerKey(document.getId(), OWNER).orElseThrow();

    assertThat(reloaded.getTitle()).isEqualTo("Guide");
    assertThat(reloaded.getFileName()).isEqualTo("Guide.pdf");
    assertThat(reloaded.getFileSize()).isEqualTo(2048L);
    assertThat(reloaded.getStatus()).isEqualTo(DocumentStatus.READY);
    assertThat(reloaded.getChunkCount()).isEqualTo(4);
    assertThat(repository.findByIdAndOwnerKey(document.getId(), OTHER)).isEmpty();
  }

  @Test
  @DisplayName("should list only the owner's documents newest first")
  void shouldListOnlyTheOwnersDocumentsNewestFirst() {
    save("Older", OWNER);
    em.flush();
    save("Newer", OWNER);
    save("Foreign", OTHER);
    flushAndClear();

    assertThat(repository.findAllByOwnerKeyOrderByCreatedAtDesc(OWNER))
        .extracting(RagDocument::getTitle)
        .containsExactly("Newer", "Older");
  }

  @Test
  @DisplayName("should delete the document only for its owner")
  void shouldDeleteTheDocumentOnlyForItsOwner() {
    RagDocument document = save("Scoped", OWNER);
    flushAndClear();

    repository.deleteByIdAndOwnerKey(document.getId(), OTHER);
    flushAndClear();
    assertThat(repository.findByIdAndOwnerKey(document.getId(), OWNER)).isPresent();

    repository.deleteByIdAndOwnerKey(document.getId(), OWNER);
    flushAndClear();
    assertThat(repository.findByIdAndOwnerKey(document.getId(), OWNER)).isEmpty();
  }

  private RagDocument save(String title, OwnerKey owner) {
    RagDocument document =
        RagDocument.startIngestion(title, title + ".pdf", 2048L, owner.getValue());
    document.completeIngestion(4);
    return repository.save(document);
  }
}
