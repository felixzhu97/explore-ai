package com.ai.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("media")
public record MediaProperties(
        StableDiffusion stableDiffusion
) {
    public record StableDiffusion(
            String apiUrl,
            String defaultModel,
            List<String> availableModels,
            GenerationConfig defaultGeneration,
            HttpConfig http
    ) {
    }

    public record GenerationConfig(
            Integer width,
            Integer height,
            Integer steps,
            Float guidanceScale,
            Integer numImages
    ) {
        public GenerationConfig {
            if (width == null) width = 512;
            if (height == null) height = 512;
            if (steps == null) steps = 25;
            if (guidanceScale == null) guidanceScale = 7.5f;
            if (numImages == null) numImages = 1;
        }
    }

    public record HttpConfig(
            Duration connectTimeout,
            Duration readTimeout,
            Duration writeTimeout
    ) {
        public HttpConfig {
            if (connectTimeout == null) connectTimeout = Duration.ofSeconds(30);
            if (readTimeout == null) readTimeout = Duration.ofMinutes(5);
            if (writeTimeout == null) writeTimeout = Duration.ofMinutes(5);
        }
    }
}
