package com.ai.mcp.infrastructure.config;

import com.ai.mcp.application.port.McpToolCallbackRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hydrates the in-app MCP registry from Spring AI MCP client tool callbacks (e.g. Fetch)
 * and exposes them for {@link com.ai.common.infrastructure.llm.ChatClientFactory}.
 */
@Configuration
public class McpClientToolsConfig {

    private static final Logger log = LoggerFactory.getLogger(McpClientToolsConfig.class);

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ToolCallback[] mcpToolCallbacks(
            ObjectProvider<ToolCallbackProvider> toolCallbackProviders,
            McpToolCallbackRegistry registry) {
        ToolCallbackProvider provider = toolCallbackProviders.getIfAvailable();
        if (provider == null) {
            log.debug("No MCP ToolCallbackProvider; chat will use local tools only");
            return new ToolCallback[0];
        }
        ToolCallback[] callbacks = provider.getToolCallbacks();
        if (callbacks == null || callbacks.length == 0) {
            log.debug("MCP client has no external tools registered");
            return new ToolCallback[0];
        }
        registry.registerToolCallbacks(callbacks, "external-mcp");
        log.info("Merged {} MCP tool callback(s) into chat", callbacks.length);
        return callbacks;
    }
}
