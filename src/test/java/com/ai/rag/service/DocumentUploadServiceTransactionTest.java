package com.ai.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ai.common.domain.model.OwnerKey;
import com.ai.common.exception.DomainException;
import com.ai.rag.domain.model.DocumentStatus;
import com.ai.rag.domain.model.ExtractedDocument;
import com.ai.rag.domain.model.RagDocument;
import com.ai.rag.domain.repository.DocumentChunkRepository;
import com.ai.rag.domain.repository.DocumentReader;
import com.ai.rag.domain.repository.DocumentRepository;
import com.ai.rag.domain.repository.DocumentTransformer;
import com.ai.rag.domain.repository.DocumentWriter;
import com.ai.testsupport.AbstractDataJpaTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Runs uploads outside a test transaction so commits and rollbacks reach the database. */
@Import(DocumentUploadService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("DocumentUploadService transactions")
class DocumentUploadServiceTransactionTest extends AbstractDataJpaTest {

  @MockitoBean private DocumentReader reader;
  @MockitoBean private DocumentTransformer transformer;
  @MockitoBean private DocumentWriter writer;
  @MockitoBean private DocumentChunkRepository chunkRepository;

  @Autowired private DocumentUploadService service;
  @Autowired private DocumentRepository documentRepository;

  @Test
  @DisplayName("should keep the document as FAILED when its text cannot be extracted")
  void shouldKeepTheDocumentAsFailedWhenItsTextCannotBeExtracted() {
    OwnerKey owner = newOwner();
    when(reader.readDocument(any(byte[].class), eq("broken.pdf")))
        .thenThrow(
            DomainException.unprocessable(
                "DOCUMENT_UNREADABLE", "Could not extract text from broken.pdf"));

    assertThatThrownBy(
            () -> service.upload("Broken", "broken.pdf", new byte[] {1, 2}, owner.value()))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "DOCUMENT_UNREADABLE");

    assertThat(documentRepository.findAllByOwnerKeyOrderByCreatedAtDesc(owner))
        .singleElement()
        .extracting(RagDocument::getStatus)
        .isEqualTo(DocumentStatus.FAILED);
  }

  @Test
  @DisplayName("should keep the document as FAILED when the file has no text")
  void shouldKeepTheDocumentAsFailedWhenTheFileHasNoText() {
    OwnerKey owner = newOwner();
    when(reader.readDocument(any(byte[].class), eq("blank.txt")))
        .thenReturn(new ExtractedDocument("  \n ", Map.of(), "blank.txt"));
    when(transformer.splitDocument(any(ExtractedDocument.class))).thenReturn(List.of());

    assertThatThrownBy(() -> service.upload("Blank", "blank.txt", "  \n ", owner.value()))
        .isInstanceOf(DomainException.class)
        .hasFieldOrPropertyWithValue("code", "DOCUMENT_UNREADABLE")
        .hasMessage("No text found in blank.txt");

    assertThat(documentRepository.findAllByOwnerKeyOrderByCreatedAtDesc(owner))
        .singleElement()
        .extracting(RagDocument::getStatus)
        .isEqualTo(DocumentStatus.FAILED);
  }

  @Test
  @DisplayName("should reject an empty file without storing a document")
  void shouldRejectAnEmptyFileWithoutStoringADocument() {
    OwnerKey owner = newOwner();

    assertThatThrownBy(() -> service.upload("Empty", "empty.txt", new byte[0], owner.value()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Uploaded file is empty");

    assertThat(documentRepository.findAllByOwnerKeyOrderByCreatedAtDesc(owner)).isEmpty();
    verifyNoInteractions(reader);
  }

  private static OwnerKey newOwner() {
    return OwnerKey.createClientKey(UUID.randomUUID().toString());
  }
}
