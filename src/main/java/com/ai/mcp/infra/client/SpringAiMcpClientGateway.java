package com.ai.mcp.infra.client;

import com.ai.mcp.domain.model.McpToolDefinition;
import com.ai.mcp.domain.repository.McpClientGateway;
import com.ai.mcp.domain.service.McpSessionRegistry;
import com.ai.mcp.domain.vo.McpServerConnection;
import com.ai.mcp.service.McpToolCallbackRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger log = LoggerFactory.getLogger(SpringAiMcpClientGateway.class);

  private final McpSessionRegistry sessionRegistry = new McpSessionRegistry();
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
    sessionRegistry
        .findActiveByServerName(serverName)
        .ifPresent(
            session -> {
              sessionRegistry.closeSession(session.id());
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
        .activeSessions()
        .forEach(
            session ->
                servers.put(
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
    sessionRegistry.clear();
    log.info("Cleared all registered MCP tools");
  }

  @Override
  public ToolCallback[] getRegisteredToolCallbacks() {
    return serverCallbacks.values().stream().flatMap(List::stream).toArray(ToolCallback[]::new);
  }
}
