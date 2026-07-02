package com.ai.audio.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Ollama-based ASR streaming.
 */
@Configuration
public class OllamaAsrConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${spring.ai.ollama.asr.model:whisper-base}")
    private String model;
}
