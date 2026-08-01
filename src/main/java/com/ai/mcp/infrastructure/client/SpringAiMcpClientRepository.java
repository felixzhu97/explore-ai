package com.ai.mcp.infrastructure.client;

import com.ai.mcp.application.port.McpToolCallbackRegistry;
import com.ai.mcp.domain.model.McpPromptDefinition;
import com.ai.mcp.domain.model.McpResourceDefinition;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.repository.McpClientRepository;
import com.ai.mcp.domain.service.McpSessionManager;
import com.ai.mcp.domain.vo.McpServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SpringAiMcpClientRepository implements McpClientRepository, McpToolCallbackRegistry {

    private static final Logger log = LoggerFactory.getLogger(SpringAiMcpClientRepository.class);

    private final McpSessionManager sessionManager = new McpSessionManager();
    private final Map<String, List<ToolCallback>> serverCallbacks = new ConcurrentHashMap<>();
    private final Map<String, List<McpToolDefinition>> serverTools = new ConcurrentHashMap<>();
    private final Map<String, List<McpResourceDefinition>> serverResources = new ConcurrentHashMap<>();
    private final Map<String, List<McpPromptDefinition>> serverPrompts = new ConcurrentHashMap<>();
    private final Map<String, CapabilityFlags> serverCapabilities = new ConcurrentHashMap<>();

    private record CapabilityFlags(boolean tools, boolean resources, boolean prompts) {}

    @Override
    public void registerToolCallbacks(ToolCallback[] tools, String serverName) {
        log.info("Registering {} tools from MCP server: {}", tools.length, serverName);
        List<ToolCallback> callbacks = List.of(tools);
        List<McpToolDefinition> definitions = new ArrayList<>();
        for (ToolCallback tool : tools) {
            var def = tool.getToolDefinition();
            definitions.add(McpToolDefinition.create(def.name(), def.description(), serverName));
        }
        serverCallbacks.put(serverName, callbacks);
        registerTools(definitions, serverName);
    }

    @Override
    public void registerTools(List<McpToolDefinition> tools, String serverName) {
        serverTools.put(serverName, List.copyOf(tools));
        ensureSession(serverName, tools.size());
        CapabilityFlags existing = serverCapabilities.getOrDefault(serverName, new CapabilityFlags(false, false, false));
        serverCapabilities.put(
                serverName,
                new CapabilityFlags(!tools.isEmpty() || existing.tools(), existing.resources(), existing.prompts()));
    }

    @Override
    public void registerResources(List<McpResourceDefinition> resources, String serverName) {
        serverResources.put(serverName, List.copyOf(resources));
        ensureSession(serverName, serverTools.getOrDefault(serverName, List.of()).size());
    }

    @Override
    public void registerPrompts(List<McpPromptDefinition> prompts, String serverName) {
        serverPrompts.put(serverName, List.copyOf(prompts));
        ensureSession(serverName, serverTools.getOrDefault(serverName, List.of()).size());
    }

    @Override
    public void updateServerCapabilities(
            String serverName,
            boolean toolsSupported,
            boolean resourcesSupported,
            boolean promptsSupported) {
        ensureSession(serverName, serverTools.getOrDefault(serverName, List.of()).size());
        serverCapabilities.put(serverName, new CapabilityFlags(toolsSupported, resourcesSupported, promptsSupported));
    }

    private void ensureSession(String serverName, int toolCount) {
        var active = sessionManager.findActiveByServerName(serverName);
        if (active.isPresent()) {
            if (active.get().toolCount() != toolCount) {
                sessionManager.closeSession(active.get().id());
                sessionManager.registerSession(serverName, toolCount);
            }
            return;
        }
        sessionManager.registerSession(serverName, toolCount);
    }

    @Override
    public List<McpToolDefinition> listTools() {
        return serverTools.values().stream().flatMap(List::stream).toList();
    }

    @Override
    public List<McpResourceDefinition> listResources() {
        return serverResources.values().stream().flatMap(List::stream).toList();
    }

    @Override
    public List<McpPromptDefinition> listPrompts() {
        return serverPrompts.values().stream().flatMap(List::stream).toList();
    }

    @Override
    public Map<String, McpServerConnection> listServers() {
        Map<String, McpServerConnection> servers = new LinkedHashMap<>();
        sessionManager.activeSessions().forEach(session -> {
            String name = session.serverName();
            CapabilityFlags caps = serverCapabilities.getOrDefault(name, new CapabilityFlags(false, false, false));
            int tools = serverTools.getOrDefault(name, List.of()).size();
            int resources = serverResources.getOrDefault(name, List.of()).size();
            int prompts = serverPrompts.getOrDefault(name, List.of()).size();
            servers.put(
                    name,
                    McpServerConnection.connected(
                            name,
                            tools,
                            resources,
                            prompts,
                            caps.tools() || tools > 0,
                            caps.resources(),
                            caps.prompts()));
        });
        return servers;
    }

    @Override
    public int toolCount() {
        return serverTools.values().stream().mapToInt(List::size).sum();
    }

    @Override
    public void clearTools() {
        serverCallbacks.clear();
        serverTools.clear();
        serverResources.clear();
        serverPrompts.clear();
        serverCapabilities.clear();
        sessionManager.clear();
        log.info("Cleared all registered MCP tools");
    }

    @Override
    public ToolCallback[] getRegisteredToolCallbacks() {
        return serverCallbacks.values().stream()
                .flatMap(List::stream)
                .toArray(ToolCallback[]::new);
    }
}
