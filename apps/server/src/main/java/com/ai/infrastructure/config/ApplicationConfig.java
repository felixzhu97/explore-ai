package com.ai.infrastructure.config;

import com.ai.application.port.AiChatPort;
import com.ai.application.port.ChatSessionRepositoryPort;
import com.ai.application.port.DocumentRepositoryPort;
import com.ai.application.port.EmbeddingPort;
import com.ai.application.port.VectorSearchPort;
import com.ai.application.service.ChatApplicationService;
import com.ai.application.service.LanguageDetectionService;
import com.ai.application.service.RagApplicationService;
import com.ai.application.usecase.DeleteDocumentUseCase;
import com.ai.application.usecase.RagChatUseCase;
import com.ai.application.usecase.SendChatMessageUseCase;
import com.ai.application.usecase.UploadDocumentUseCase;
import com.ai.domain.service.AiChatService;
import com.ai.infrastructure.adapter.ai.SpringAiChatAdapter;
import com.ai.infrastructure.adapter.ai.SpringAiChatService;
import com.ai.infrastructure.adapter.embedding.MockEmbeddingAdapter;
import com.ai.infrastructure.adapter.embedding.OllamaEmbeddingAdapter;
import com.ai.infrastructure.adapter.persistence.InMemoryChatSessionRepository;
import com.ai.infrastructure.adapter.persistence.JpaDocumentRepository;
import com.ai.infrastructure.adapter.vector.PgVectorAdapter;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.unit.DataSize;

/**
 * Application configuration class - manages dependency injection.
 * Connects infrastructure layer with domain/application layers.
 */
@Configuration
public class ApplicationConfig {

    /**
     * Configure multipart upload settings explicitly to ensure limits are applied.
     * This bean explicitly sets 50MB limits to ensure they are not overridden by
     * environment variables or other configuration sources.
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(50));
        factory.setMaxRequestSize(DataSize.ofMegabytes(50));
        factory.setLocation("/tmp/uploads");
        return factory.createMultipartConfig();
    }

    @Bean
    public AiChatService aiChatService(SpringAiChatService springAiChatService) {
        return springAiChatService;
    }

    @Bean
    public AiChatPort aiChatPort(SpringAiChatAdapter springAiChatAdapter) {
        return springAiChatAdapter;
    }

    @Bean
    public InMemoryChatSessionRepository chatSessionRepository() {
        return new InMemoryChatSessionRepository();
    }

    @Bean
    public SendChatMessageUseCase sendChatMessageUseCase(
            ChatSessionRepositoryPort repositoryPort,
            AiChatPort aiChatPort) {
        return new SendChatMessageUseCase(repositoryPort, aiChatPort);
    }

    @Bean
    public ChatApplicationService chatApplicationService(
            ChatSessionRepositoryPort repositoryPort,
            AiChatPort aiChatPort,
            SendChatMessageUseCase sendChatMessageUseCase) {
        return new ChatApplicationService(repositoryPort, aiChatPort, sendChatMessageUseCase);
    }

    // RAG Infrastructure Beans

    @Bean
    public LanguageDetectionService languageDetectionService() {
        return new LanguageDetectionService();
    }

    @Bean
    public JpaDocumentRepository jpaDocumentRepository(
            com.ai.infrastructure.adapter.persistence.SpringDataDocumentRepository documentRepository,
            com.ai.infrastructure.adapter.persistence.SpringDataChunkRepository chunkRepository,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new JpaDocumentRepository(documentRepository, chunkRepository, objectMapper);
    }

    @Bean
    public DocumentRepositoryPort documentRepositoryPort(JpaDocumentRepository jpaDocumentRepository) {
        return jpaDocumentRepository;
    }

    @Bean
    public EmbeddingPort embeddingPort(
            org.springframework.beans.factory.ObjectProvider<MockEmbeddingAdapter> mockProvider,
            org.springframework.beans.factory.ObjectProvider<OllamaEmbeddingAdapter> ollamaProvider,
            org.springframework.core.env.Environment env) {
        
        boolean useMock = Boolean.parseBoolean(
            env.getProperty("rag.mock.embeddings", "false"));
        
        if (useMock) {
            MockEmbeddingAdapter mock = mockProvider.getIfAvailable();
            if (mock != null) {
                return mock;
            }
            throw new IllegalStateException("Mock embedding adapter not available but rag.mock.embeddings=true");
        }
        
        // Try Ollama first (local embedding)
        OllamaEmbeddingAdapter ollama = ollamaProvider.getIfAvailable();
        if (ollama != null) {
            return ollama;
        }
        
        // Final fallback to mock
        MockEmbeddingAdapter fallbackMock = mockProvider.getIfAvailable();
        if (fallbackMock != null) {
            return fallbackMock;
        }
        
        throw new IllegalStateException("No embedding adapter available");
    }

    @Bean
    public VectorSearchPort vectorSearchPort(PgVectorAdapter pgVectorAdapter) {
        return pgVectorAdapter;
    }

    // RAG Use Cases

    @Bean
    public UploadDocumentUseCase uploadDocumentUseCase(
            DocumentRepositoryPort documentRepositoryPort,
            EmbeddingPort embeddingPort,
            VectorSearchPort vectorSearchPort,
            org.springframework.core.env.Environment env) {
        int chunkSize = Integer.parseInt(env.getProperty("rag.chunk.size", "500"));
        int chunkOverlap = Integer.parseInt(env.getProperty("rag.chunk.overlap", "50"));
        return new UploadDocumentUseCase(documentRepositoryPort, embeddingPort, vectorSearchPort, chunkSize, chunkOverlap);
    }

    @Bean
    public DeleteDocumentUseCase deleteDocumentUseCase(DocumentRepositoryPort documentRepositoryPort) {
        return new DeleteDocumentUseCase(documentRepositoryPort);
    }

    @Bean
    public RagChatUseCase ragChatUseCase(EmbeddingPort embeddingPort, VectorSearchPort vectorSearchPort) {
        return new RagChatUseCase(embeddingPort, vectorSearchPort);
    }

    @Bean
    public RagApplicationService ragApplicationService(
            UploadDocumentUseCase uploadDocumentUseCase,
            DeleteDocumentUseCase deleteDocumentUseCase,
            RagChatUseCase ragChatUseCase,
            DocumentRepositoryPort documentRepositoryPort) {
        return new RagApplicationService(uploadDocumentUseCase, deleteDocumentUseCase, ragChatUseCase, documentRepositoryPort);
    }
}
