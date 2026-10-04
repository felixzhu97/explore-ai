package com.ai.rag.infra.storage;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.common.infra.persistence.OwnerPartitionScope;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.repository.DocumentRepository;
import com.ai.rag.domain.vo.DocumentId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Document repository adapter delegating to Spring Data JPA. */
@Component
public class JpaDocumentRepository implements DocumentRepository {

  private final SpringDataDocumentRepository delegate;
  private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

  private final OwnerPartitionScope ownerPartition;

  public JpaDocumentRepository(
      SpringDataDocumentRepository delegate, OwnerPartitionScope ownerPartition) {
    this.delegate = delegate;
    this.ownerPartition = ownerPartition;
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
    return ownerPartition.findOne(
        OwnerKey.parse(ownerKey), () -> delegate.findById(DocumentId.of(id)));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Document> findAll() {
    return delegate.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Document> findAllByOwnerKey(String ownerKey) {
    return ownerPartition.apply(OwnerKey.parse(ownerKey), () -> delegate.findAll(NEWEST_FIRST));
  }

  @Override
  @Transactional
  public void delete(UUID id) {
    delegate.deleteById(DocumentId.of(id));
  }

  @Override
  @Transactional
  public void deleteByIdAndOwnerKey(UUID id, String ownerKey) {
    ownerPartition.run(OwnerKey.parse(ownerKey), () -> delegate.deleteById(DocumentId.of(id)));
  }
}
