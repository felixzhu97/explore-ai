package com.ai.infrastructure.adapter.web;

import com.ai.infrastructure.config.WebSearchProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for web search components.
 */
@Configuration
public class WebSearchConfig {

    @Bean
    public WebSearchPort webSearchPort(DuckDuckGoWebSearchAdapter adapter) {
        return adapter;
    }
}
