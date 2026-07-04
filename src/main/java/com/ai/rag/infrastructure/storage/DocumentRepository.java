package com.ai.rag.infrastructure.storage;

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
        DocumentEntity entity = DocumentEntity.fromDomain(document);
        // Flush immediately to ensure FK constraint is satisfied
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
    public List<Document> findAll() {
        return delegate.findAll().stream().map(DocumentEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        delegate.deleteById(id);
    }
}
