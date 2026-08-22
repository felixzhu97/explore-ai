package com.ai.rag.infra.storage;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.repository.IDocumentRepository;
import com.ai.rag.domain.vo.DocumentId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Document repository adapter delegating to Spring Data JPA. */
@Component
public class DocumentRepository implements IDocumentRepository {

  private final SpringDataDocumentRepository delegate;

  /** Documentation. */
  public DocumentRepository(SpringDataDocumentRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public Document save(Document document) {
    return save(document, OwnerKey.LEGACY_ORPHAN.value());
  }

  @Override
  @Transactional
  public Document save(Document document, String ownerKey) {
    return delegate.saveAndFlush(document);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Document> findById(UUID id) {
    return delegate.findById(DocumentId.of(id));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Document> findByIdAndOwnerKey(UUID id, String ownerKey) {
    return delegate.findByIdAndOwnerKeyValue(id.toString(), ownerKey);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Document> findAll() {
    return delegate.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Document> findAllByOwnerKey(String ownerKey) {
    return delegate.findByOwnerKeyValueOrderByCreatedAtDesc(ownerKey);
  }

  @Override
  @Transactional
  public void delete(UUID id) {
    delegate.deleteById(DocumentId.of(id));
  }

  @Override
  @Transactional
  public void deleteByIdAndOwnerKey(UUID id, String ownerKey) {
    delegate.deleteByIdAndOwnerKeyValue(id.toString(), ownerKey);
  }
}
