package com.ai.mcp.infra.config;

import io.modelcontextprotocol.client.McpClient;
import java.time.Duration;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentation. */
@Configuration
@ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = "enabled", havingValue = "true")
public class McpClientResilienceConfig {

  @Bean
  McpClientCustomizer<McpClient.SyncSpec> mcpClientTimeoutCustomizer(
      @Value("${spring.ai.mcp.client.request-timeout:20s}") Duration requestTimeout) {
    return (serverConfigurationName, spec) -> {
      spec.requestTimeout(requestTimeout);
      spec.initializationTimeout(requestTimeout);
    };
  }
}
