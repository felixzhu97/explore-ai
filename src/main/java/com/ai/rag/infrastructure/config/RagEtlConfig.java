package com.ai.rag.infrastructure.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ETL wiring for document ingest ({@link TokenTextSplitter}).
 */
@Configuration
public class RagEtlConfig {

    @Bean
    public TokenTextSplitter ragTokenTextSplitter(RagProperties properties) {
        return TokenTextSplitter.builder()
                .withChunkSize(properties.getChunk().getSize())
                .build();
    }
}
