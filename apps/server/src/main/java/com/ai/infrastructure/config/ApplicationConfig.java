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
import com.ai.infrastructure.adapter.ai.DocumentSearchTool;
import com.ai.infrastructure.adapter.ai.SpringAiChatAdapter;
import com.ai.infrastructure.adapter.ai.SpringAiChatService;
import com.ai.infrastructure.adapter.embedding.MockEmbeddingAdapter;
import com.ai.infrastructure.adapter.embedding.OllamaEmbeddingAdapter;
import com.ai.infrastructure.adapter.monitor.OshiSystemInfoProvider;
import com.ai.infrastructure.adapter.monitor.SystemInfoProvider;
import com.ai.infrastructure.adapter.persistence.InMemoryChatSessionRepository;
import com.ai.infrastructure.adapter.persistence.JpaDocumentRepository;
import com.ai.infrastructure.adapter.vector.PgVectorAdapter;
import com.ai.infrastructure.adapter.web.DuckDuckGoWebSearchAdapter;
import com.ai.infrastructure.adapter.web.WebSearchPort;
import oshi.SystemInfo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Application configuration class - manages dependency injection.
 * Connects infrastructure layer with domain/application layers.
 */
@Configuration
@EnableConfigurationProperties({MonitorProperties.class, WebSearchProperties.class})
public class ApplicationConfig {

    /**
     * Configure ChatMemory for conversation history management.
     * Uses MessageWindowChatMemory which keeps the last N messages (default 20).
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().build();
    }

    /**
     * Primary ChatModel bean to resolve ambiguity when both Ollama and OpenAI
     * ChatModel beans are auto-configured by Spring AI.
     * Spring AI's ChatClientAutoConfiguration requires a single ChatModel.
     */
    @Bean
    @Primary
    public ChatModel primaryChatModel(@Qualifier("openAiChatModel") ChatModel openAiChatModel) {
        return openAiChatModel;
    }

    /**
     * Configure MessageChatMemoryAdvisor for automatic conversation history injection.
     */
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    /**
     * Configure ChatClient with memory and tool support.
     * Uses the ChatModel directly (injected via @Qualifier) to build the ChatClient.
     */
    @Bean
    public ChatClient chatClient(
                                 @Qualifier("openAiChatModel") org.springframework.ai.chat.model.ChatModel chatModel,
                                 MessageChatMemoryAdvisor memoryAdvisor,
                                 DocumentSearchTool documentSearchTool) {
        return ChatClient.builder(chatModel)
            .defaultAdvisors(memoryAdvisor)
            .defaultTools(documentSearchTool)
            .build();
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
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
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

    // === Monitor Tools ===

    /**
     * Enable configuration properties scanning for monitor and web search settings.
     */
    @Bean
    public SystemInfoProvider systemInfoProvider(MonitorProperties monitorProperties) {
        return new OshiSystemInfoProvider(monitorProperties);
    }

    // === Web Search ===

    @Bean
    public WebSearchPort webSearchPort(WebSearchProperties webSearchProperties) {
        return new DuckDuckGoWebSearchAdapter(webSearchProperties);
    }
}
