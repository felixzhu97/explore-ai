package com.ai.mcp.infrastructure.client;

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
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class SpringAiMcpClientRepository implements McpClientRepository {

    private static final Logger log = LoggerFactory.getLogger(SpringAiMcpClientRepository.class);

    private final McpSessionManager sessionManager = new McpSessionManager();
    private final CopyOnWriteArrayList<ToolCallback> registeredCallbacks = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<McpToolDefinition> registeredTools = new CopyOnWriteArrayList<>();

    public void registerToolCallbacks(ToolCallback[] tools, String serverName) {
        log.info("Registering {} tools from MCP server: {}", tools.length, serverName);
        List<McpToolDefinition> definitions = new ArrayList<>();
        for (ToolCallback tool : tools) {
            registeredCallbacks.add(tool);
            var def = tool.getToolDefinition();
            definitions.add(McpToolDefinition.create(def.name(), def.description()));
        }
        registerTools(definitions, serverName);
    }

    @Override
    public void registerTools(List<McpToolDefinition> tools, String serverName) {
        tools.forEach(McpToolDefinition::validate);
        registeredTools.addAll(tools);
        sessionManager.registerSession(serverName, tools.size());
    }

    @Override
    public List<McpToolDefinition> listTools() {
        return List.copyOf(registeredTools);
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
        return registeredTools.size();
    }

    @Override
    public void clearTools() {
        registeredCallbacks.clear();
        registeredTools.clear();
        sessionManager.clear();
        log.info("Cleared all registered MCP tools");
    }

    public ToolCallback[] getRegisteredToolCallbacks() {
        return registeredCallbacks.toArray(new ToolCallback[0]);
    }
}
