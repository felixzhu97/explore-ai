package com.ai.rag.config;

import com.ai.rag.application.service.RagChatApplicationService;
import com.ai.rag.application.service.RagDocumentApplicationService;
import com.ai.rag.domain.DocumentRepository;
import com.ai.rag.domain.EmbeddingClient;
import com.ai.rag.domain.VectorStore;
import com.ai.rag.domain.service.ChunkingService;
import com.ai.rag.infrastructure.embedding.OpenAiEmbeddingClient;
import com.ai.rag.infrastructure.persistence.InMemoryDocumentRepository;
import com.ai.rag.infrastructure.rag.RagProperties;
import com.ai.rag.infrastructure.vector.InMemoryVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for RAG infrastructure components.
 * Uses in-memory vector store by default.
 * For production with Qdrant, use the vector infrastructure module.
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RagConfiguration.class);

    @Bean
    public ChunkingService chunkingService() {
        return new ChunkingService();
    }

    @Bean
    public DocumentRepository documentRepository() {
        return new InMemoryDocumentRepository();
    }

    @Bean
    public EmbeddingClient embeddingClient() {
        log.warn("EmbeddingClient using placeholder implementation");
        return new OpenAiEmbeddingClient();
    }

    @Bean
    public VectorStore vectorStore() {
        log.info("Initializing InMemoryVectorStore (development mode)");
        return new InMemoryVectorStore();
    }

    @Bean
    public RagDocumentApplicationService ragDocumentApplicationService(
            DocumentRepository documentRepository,
            VectorStore vectorStore,
            ChunkingService chunkingService,
            RagProperties properties
    ) {
        return new RagDocumentApplicationService(documentRepository, vectorStore, chunkingService, properties);
    }

    @Bean
    public RagChatApplicationService ragChatApplicationService(VectorStore vectorStore) {
        return new RagChatApplicationService(vectorStore);
    }
}
