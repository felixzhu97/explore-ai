package com.ai.rag.infrastructure.storage;

import com.ai.rag.domain.model.Document;
import com.ai.rag.domain.repository.IDocumentRepository;
import org.springframework.stereotype.Component;

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
    public Document save(Document document) {
        return delegate.save(DocumentEntity.fromDomain(document)).toDomain();
    }

    @Override
    public Optional<Document> findById(UUID id) {
        return delegate.findById(id).map(DocumentEntity::toDomain);
    }

    @Override
    public List<Document> findAll() {
        return delegate.findAll().stream().map(DocumentEntity::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        delegate.deleteById(id);
    }
}
