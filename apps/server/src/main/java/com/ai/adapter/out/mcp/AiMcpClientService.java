package com.ai.adapter.out.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Client service for managing connections to external MCP servers.
 * Provides access to tools exposed by MCP servers.
 */
@Service
public class AiMcpClientService {

    private static final Logger log = LoggerFactory.getLogger(AiMcpClientService.class);

    private final List<ToolCallback> registeredTools = new ArrayList<>();
    private final Map<String, ServerInfo> connectedServers = new ConcurrentHashMap<>();

    public AiMcpClientService() {
    }

    /**
     * Register tools from an MCP server.
     */
    public void registerTools(ToolCallback[] tools, String serverName) {
        log.info("Registering {} tools from MCP server: {}", tools.length, serverName);
        for (ToolCallback tool : tools) {
            registeredTools.add(tool);
        }
        connectedServers.put(serverName, new ServerInfo(serverName, tools.length, "CONNECTED"));
    }

    /**
     * Get all registered tools from MCP servers.
     */
    public List<ToolCallback> getRegisteredTools() {
        return new ArrayList<>(registeredTools);
    }

    /**
     * Get tool definitions.
     */
    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> definitions = new ArrayList<>();
        for (ToolCallback tool : registeredTools) {
            definitions.add(tool.getToolDefinition());
        }
        return definitions;
    }

    /**
     * Get connected server information.
     */
    public Map<String, ServerInfo> getConnectedServers() {
        return new ConcurrentHashMap<>(connectedServers);
    }

    /**
     * Get total count of registered tools.
     */
    public int getTotalToolCount() {
        return registeredTools.size();
    }

    /**
     * Clear all registered tools.
     */
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
