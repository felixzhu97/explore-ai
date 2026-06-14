package com.ai.infrastructure.config;

import com.ai.application.port.ToolRegistryPort;
import com.ai.infrastructure.adapter.tool.InMemoryToolRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for tool registry.
 */
@Configuration
public class ToolRegistryConfig {

    @Bean
    @Primary
    public ToolRegistryPort toolRegistry(InMemoryToolRegistry registry) {
        return registry;
    }
}
