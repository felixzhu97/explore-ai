package com.ai.rag.config;

import com.ai.rag.store.QdrantEmbeddingStore;
import com.qdrant.client.QdrantClient;
import com.qdrant.client.QdrantGrpcClient;
import dev.langchain4j.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Qdrant vector database connection and embedding store.
 */
@Configuration
public class QdrantConfig {

    private static final Logger log = LoggerFactory.getLogger(QdrantConfig.class);

    @Value("${rag.qdrant.host:localhost}")
    private String host;

    @Value("${rag.qdrant.port:6333}")
    private Integer port;

    @Value("${rag.qdrant.collection-name:documents}")
    private String collectionName;

    @Value("${rag.qdrant.embedding-dimension:384}")
    private Integer embeddingDimension;

    @Value("${rag.qdrant.api-key:}")
    private String apiKey;

    @Bean
    public QdrantClient qdrantClient() {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, false);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.withApiKey(apiKey);
        }

        QdrantClient client = new QdrantClient(builder.build());
        log.info("Qdrant client configured for {}:{}", host, port);

        return client;
    }

    @Bean
    public QdrantEmbeddingStore qdrantEmbeddingStore(
            QdrantClient qdrantClient,
            @Value("${rag.qdrant.embedding-dimension:384}") int embeddingDimension
    ) {
        QdrantEmbeddingStore store = new QdrantEmbeddingStore(
                qdrantClient,
                collectionName,
                embeddingDimension
        );
        log.info("QdrantEmbeddingStore configured for collection: {}", collectionName);
        return store;
    }
}
