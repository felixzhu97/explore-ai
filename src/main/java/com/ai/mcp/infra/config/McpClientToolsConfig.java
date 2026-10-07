package com.ai.mcp.infra.config;

import com.ai.mcp.service.McpToolCallbackRegistry;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hydrates the in-app MCP registry from Spring AI MCP client tool callbacks (e.g. Fetch) and
 * exposes them for {@link com.ai.common.infra.llm.ChatClientFactory}.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-mcp",
    havingValue = "true",
    matchIfMissing = false)
public class McpClientToolsConfig {

  /** Loads the MCP tool callbacks and registers them. */
  @Bean
  @ConditionalOnProperty(
      prefix = "spring.ai.mcp.client",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = false)
  public ToolCallback[] mcpToolCallbacks(
      ObjectProvider<ToolCallbackProvider> toolCallbackProviders,
      McpToolCallbackRegistry registry) {
    try {
      ToolCallbackProvider provider = toolCallbackProviders.getIfAvailable();
      if (provider == null) {
        return new ToolCallback[0];
      }
      ToolCallback[] callbacks = provider.getToolCallbacks();
      if (callbacks == null || callbacks.length == 0) {
        return new ToolCallback[0];
      }
      registry.registerToolCallbacks(callbacks, "external-mcp");
      return callbacks;
    } catch (RuntimeException ex) {
      return new ToolCallback[0];
    }
  }
}
