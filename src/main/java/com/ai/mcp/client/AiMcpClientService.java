package com.ai.mcp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MCP Client service for managing connections to external MCP servers.
 * Provides access to tools exposed by MCP servers.
 */
@Service
public class AiMcpClientService {

    private static final Logger log = LoggerFactory.getLogger(AiMcpClientService.class);

    private final CopyOnWriteArrayList<ToolCallback> registeredTools = new CopyOnWriteArrayList<>();
    private final Map<String, ServerInfo> connectedServers = new ConcurrentHashMap<>();

    public AiMcpClientService() {
    }

    public void registerTools(ToolCallback[] tools, String serverName) {
        log.info("Registering {} tools from MCP server: {}", tools.length, serverName);
        for (ToolCallback tool : tools) {
            registeredTools.add(tool);
        }
        connectedServers.put(serverName, new ServerInfo(serverName, tools.length, "CONNECTED"));
    }

    public List<ToolCallback> getRegisteredTools() {
        return new ArrayList<>(registeredTools);
    }

    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> definitions = new ArrayList<>();
        for (ToolCallback tool : registeredTools) {
            definitions.add(tool.getToolDefinition());
        }
        return definitions;
    }

    public Map<String, ServerInfo> getConnectedServers() {
        return new ConcurrentHashMap<>(connectedServers);
    }

    public int getTotalToolCount() {
        return registeredTools.size();
    }

    public void clearTools() {
        registeredTools.clear();
        connectedServers.clear();
        log.info("Cleared all registered MCP tools");
    }

    /**
     * Server connection information record.
     */
    public record ServerInfo(String name, int toolCount, String status) {
    }
}
