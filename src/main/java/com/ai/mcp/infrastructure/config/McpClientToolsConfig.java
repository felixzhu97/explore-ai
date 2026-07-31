package com.ai.mcp.infrastructure.config;

import com.ai.mcp.application.port.McpToolCallbackRegistry;
import com.ai.mcp.domain.model.McpPromptDefinition;
import com.ai.mcp.domain.model.McpResourceDefinition;
import com.ai.mcp.domain.repository.McpClientRepository;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hydrates the in-app MCP registry from Spring AI MCP clients (per-server)
 * and exposes tool callbacks for {@link com.ai.common.infrastructure.llm.ChatClientFactory}.
 */
@Configuration
public class McpClientToolsConfig {

    private static final Logger log = LoggerFactory.getLogger(McpClientToolsConfig.class);

    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.mcp.client", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ToolCallback[] mcpToolCallbacks(
            ObjectProvider<List<McpSyncClient>> mcpSyncClients,
            ObjectProvider<ToolCallbackProvider> toolCallbackProviders,
            McpToolCallbackRegistry registry,
            McpClientRepository clientRepository) {
        try {
            List<McpSyncClient> clients = mcpSyncClients.getIfAvailable(List::of);
            if (clients != null && !clients.isEmpty()) {
                return registerPerClient(clients, registry, clientRepository);
            }
            return registerFromProvider(toolCallbackProviders, registry);
        } catch (RuntimeException ex) {
            log.warn("MCP tool registration failed; chat will use local tools only: {}", ex.getMessage());
            return new ToolCallback[0];
        }
    }

    private ToolCallback[] registerPerClient(
            List<McpSyncClient> clients,
            McpToolCallbackRegistry registry,
            McpClientRepository clientRepository) {
        List<ToolCallback> all = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        int index = 0;
        for (McpSyncClient client : clients) {
            String serverName = uniqueServerName(resolveServerName(client, index++), usedNames);
            usedNames.add(serverName);
            List<ToolCallback> callbacks = SyncMcpToolCallbackProvider.syncToolCallbacks(List.of(client));
            ToolCallback[] array = callbacks.toArray(ToolCallback[]::new);
            if (array.length > 0) {
                registry.registerToolCallbacks(array, serverName);
                all.addAll(callbacks);
            } else {
                clientRepository.registerTools(List.of(), serverName);
            }
            registerRemotePrimitives(client, serverName, array.length > 0, clientRepository);
            log.info(
                    "Registered MCP server '{}' with {} tool(s)",
                    serverName,
                    array.length);
        }
        return all.toArray(ToolCallback[]::new);
    }

    private ToolCallback[] registerFromProvider(
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
        // Fallback when List<McpSyncClient> is unavailable — still avoid "external-mcp".
        registry.registerToolCallbacks(callbacks, "mcp-client");
        log.info("Merged {} MCP tool callback(s) into chat under server 'mcp-client'", callbacks.length);
        return callbacks;
    }

    private void registerRemotePrimitives(
            McpSyncClient client,
            String serverName,
            boolean hasTools,
            McpClientRepository clientRepository) {
        boolean toolsSupported = hasTools;
        boolean resourcesSupported = false;
        boolean promptsSupported = false;
        McpSchema.ServerCapabilities capabilities = null;
        try {
            capabilities = client.getServerCapabilities();
        } catch (RuntimeException ex) {
            log.debug("Could not read capabilities for {}: {}", serverName, ex.getMessage());
        }
        if (capabilities != null) {
            toolsSupported = hasTools || capabilities.tools() != null;
            resourcesSupported = capabilities.resources() != null;
            promptsSupported = capabilities.prompts() != null;
        }

        if (resourcesSupported) {
            try {
                McpSchema.ListResourcesResult result = client.listResources();
                List<McpResourceDefinition> resources = result.resources().stream()
                        .map(resource -> McpResourceDefinition.create(
                                resource.uri(),
                                resource.name() != null ? resource.name() : resource.uri(),
                                resource.description() != null ? resource.description() : "",
                                serverName))
                        .toList();
                clientRepository.registerResources(resources, serverName);
            } catch (RuntimeException ex) {
                log.debug("listResources failed for {}: {}", serverName, ex.getMessage());
                clientRepository.registerResources(List.of(), serverName);
            }
        } else {
            clientRepository.registerResources(List.of(), serverName);
        }

        if (promptsSupported) {
            try {
                McpSchema.ListPromptsResult result = client.listPrompts();
                List<McpPromptDefinition> prompts = result.prompts().stream()
                        .map(prompt -> McpPromptDefinition.create(
                                prompt.name(),
                                prompt.description() != null ? prompt.description() : "",
                                serverName))
                        .toList();
                clientRepository.registerPrompts(prompts, serverName);
            } catch (RuntimeException ex) {
                log.debug("listPrompts failed for {}: {}", serverName, ex.getMessage());
                clientRepository.registerPrompts(List.of(), serverName);
            }
        } else {
            clientRepository.registerPrompts(List.of(), serverName);
        }

        clientRepository.updateServerCapabilities(
                serverName,
                toolsSupported,
                resourcesSupported,
                promptsSupported);
    }

    private static String resolveServerName(McpSyncClient client, int index) {
        try {
            McpSchema.Implementation serverInfo = client.getServerInfo();
            if (serverInfo != null && serverInfo.name() != null && !serverInfo.name().isBlank()) {
                return serverInfo.name().trim();
            }
        } catch (RuntimeException ignored) {
            // not initialized yet
        }
        try {
            McpSchema.Implementation clientInfo = client.getClientInfo();
            if (clientInfo != null && clientInfo.name() != null && !clientInfo.name().isBlank()) {
                return clientInfo.name().trim();
            }
        } catch (RuntimeException ignored) {
            // ignore
        }
        return "mcp-server-" + (index + 1);
    }

    private static String uniqueServerName(String base, Set<String> used) {
        if (!used.contains(base)) {
            return base;
        }
        int suffix = 2;
        while (used.contains(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }
}
