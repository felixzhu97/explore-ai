package com.ai.mcp.infrastructure.client;

import com.ai.mcp.application.port.McpToolCallbackRegistry;
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

    @Override
    public void registerToolCallbacks(ToolCallback[] tools, String serverName) {
        log.info("Registering {} tools from MCP server: {}", tools.length, serverName);
        List<ToolCallback> callbacks = List.of(tools);
        List<McpToolDefinition> definitions = new ArrayList<>();
        for (ToolCallback tool : tools) {
            var def = tool.getToolDefinition();
            definitions.add(McpToolDefinition.create(def.name(), def.description()));
        }
        serverCallbacks.put(serverName, callbacks);
        registerTools(definitions, serverName);
    }

    @Override
    public void registerTools(List<McpToolDefinition> tools, String serverName) {
        sessionManager.findActiveByServerName(serverName).ifPresent(session -> {
            sessionManager.closeSession(session.id());
        });
        serverTools.put(serverName, List.copyOf(tools));
        sessionManager.registerSession(serverName, tools.size());
    }

    @Override
    public List<McpToolDefinition> listTools() {
        return serverTools.values().stream().flatMap(List::stream).toList();
    }

    @Override
    public Map<String, McpServerConnection> listServers() {
        Map<String, McpServerConnection> servers = new LinkedHashMap<>();
        sessionManager.activeSessions().forEach(session -> servers.put(
                session.serverName(),
                McpServerConnection.connected(session.serverName(), session.toolCount())));
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
