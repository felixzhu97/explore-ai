package com.ai.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to enable AI Agents module components in the Gateway.
 * This allows the Gateway to use AI Agents endpoints and services.
 */
@Configuration
@ComponentScan(basePackages = {
        "com.ai.agents.service",
        "com.ai.agents.config",
        "com.ai.agents.domain"
})
public class AiAgentsImportConfiguration {
    // AI Agents module is imported and auto-configured
}
