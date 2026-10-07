package com.ai.mcp.infra.client;

import com.ai.mcp.domain.model.McpServerConnection;
import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.repository.McpClientGateway;
import com.ai.mcp.service.McpToolCallbackRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** In-memory registry of tools and sessions for connected MCP servers. */
@Repository
@ConditionalOnProperty(
    prefix = "launchdarkly.bootstrap",
    name = "module-mcp",
    havingValue = "true",
    matchIfMissing = false)
public class SpringAiMcpClientGateway implements McpClientGateway, McpToolCallbackRegistry {

  private final McpSessionRegistry sessionRegistry = new McpSessionRegistry();
  private final Map<String, List<ToolCallback>> serverCallbacks = new ConcurrentHashMap<>();
  private final Map<String, List<McpToolDefinition>> serverTools = new ConcurrentHashMap<>();

  @Override
  public void registerToolCallbacks(ToolCallback[] tools, String serverName) {
    List<ToolCallback> callbacks = List.of(tools);
    List<McpToolDefinition> definitions = new ArrayList<>();
    for (ToolCallback tool : tools) {
      var def = tool.getToolDefinition();
      definitions.add(McpToolDefinition.createDefinition(def.name(), def.description()));
    }
    serverCallbacks.put(serverName, callbacks);
    registerTools(definitions, serverName);
  }

  @Override
  public void registerTools(List<McpToolDefinition> tools, String serverName) {
    sessionRegistry
        .findActiveByServerName(serverName)
        .ifPresent(
            session -> {
              sessionRegistry.closeSession(session.getId());
            });
    serverTools.put(serverName, List.copyOf(tools));
    sessionRegistry.registerSession(serverName, tools.size());
  }

  @Override
  public List<McpToolDefinition> listTools() {
    return serverTools.values().stream().flatMap(List::stream).toList();
  }

  @Override
  public Map<String, McpServerConnection> listServers() {
    Map<String, McpServerConnection> servers = new LinkedHashMap<>();
    sessionRegistry
        .listActiveSessions()
        .forEach(
            session ->
                servers.put(
                    session.getServerName(),
                    McpServerConnection.createConnectedServer(
                        session.getServerName(), session.getToolCount())));
    return servers;
  }

  @Override
  public int countTools() {
    return serverTools.values().stream().mapToInt(List::size).sum();
  }

  @Override
  public void clearTools() {
    serverCallbacks.clear();
    serverTools.clear();
    sessionRegistry.clearSessions();
  }

  @Override
  public ToolCallback[] getRegisteredToolCallbacks() {
    return serverCallbacks.values().stream().flatMap(List::stream).toArray(ToolCallback[]::new);
  }
}
