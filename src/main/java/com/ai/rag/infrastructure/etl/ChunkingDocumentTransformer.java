package com.ai.rag.infrastructure.etl;

import com.ai.rag.domain.model.RawDocument;
import com.ai.rag.domain.port.DocumentTransformer;
import com.ai.rag.application.usecase.ChunkingService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Infrastructure adapter for chunking documents.
 */
@Component
public class ChunkingDocumentTransformer implements DocumentTransformer {

    private final ChunkingService chunkingService;

    public ChunkingDocumentTransformer(ChunkingService chunkingService) {
        this.chunkingService = chunkingService;
    }

    @Override
    public List<RawDocument> transform(RawDocument document) {
        List<String> chunks = chunkingService.chunk(document.content());
        return chunks.stream()
                .map(chunk -> new RawDocument(chunk, document.metadata(), document.source()))
                .collect(Collectors.toList());
    }
}
