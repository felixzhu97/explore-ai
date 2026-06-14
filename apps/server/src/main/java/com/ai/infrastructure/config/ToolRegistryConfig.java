package com.ai.infrastructure.config;

import com.ai.application.port.ToolRegistryPort;
import com.ai.infrastructure.adapter.tool.InMemoryToolRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for tool registry.
 */
@Configuration
public class ToolRegistryConfig {

    @Bean
    public ToolRegistryPort toolRegistry(InMemoryToolRegistry registry) {
        return registry;
    }
}
