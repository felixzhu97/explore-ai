package com.ai.rag.infrastructure.config;

import com.ai.rag.infrastructure.vector.H2DocumentVectorStore;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagAdvisorConfig {

    @Bean
    public VectorStoreDocumentRetriever vectorStoreDocumentRetriever(
            H2DocumentVectorStore vectorStore,
            @Value("${spring.ai.rag.retrieval.top-k:5}") int topK) {
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(topK)
                .build();
    }

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
            VectorStoreDocumentRetriever documentRetriever) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }
}
