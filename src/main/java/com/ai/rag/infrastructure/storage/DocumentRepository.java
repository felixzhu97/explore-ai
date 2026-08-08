package com.ai.rag.infrastructure.storage;

import com.ai.common.domain.vo.OwnerKey;
import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.repository.IDocumentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Document repository adapter - delegates to SpringData, maps entity to domain.
 */
@Component
public class DocumentRepository implements IDocumentRepository {

    private final SpringDataDocumentRepository delegate;

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
        DocumentEntity entity = DocumentEntity.fromDomain(document, ownerKey);
        entity = delegate.saveAndFlush(entity);
        return entity.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findById(UUID id) {
        return delegate.findById(id).map(DocumentEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Document> findByIdAndOwnerKey(UUID id, String ownerKey) {
        return delegate.findByIdAndOwnerKey(id, ownerKey).map(DocumentEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findAll() {
        return delegate.findAll().stream().map(DocumentEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Document> findAllByOwnerKey(String ownerKey) {
        return delegate.findByOwnerKeyOrderByCreatedAtDesc(ownerKey).stream()
                .map(DocumentEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        delegate.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByIdAndOwnerKey(UUID id, String ownerKey) {
        delegate.deleteByIdAndOwnerKey(id, ownerKey);
    }
}
