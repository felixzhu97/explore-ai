package com.ai.shared.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web CORS configuration.
 * CORS origins are externalized via application properties.
 * Uses allowedOriginPatterns to support wildcards.
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin-patterns:http://localhost:4200,http://localhost:3000}")
    private String[] allowedOriginPatterns;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] trimmedPatterns = java.util.Arrays.stream(allowedOriginPatterns)
                .map(String::trim)
                .toArray(String[]::new);
        registry.addMapping("/api/**").allowedOriginPatterns(trimmedPatterns);
    }
}
