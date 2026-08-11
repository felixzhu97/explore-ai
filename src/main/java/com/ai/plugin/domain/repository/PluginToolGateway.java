package com.ai.plugin.domain.repository;

import org.springframework.ai.tool.ToolCallback;

import java.util.List;

/**
 * Resolves ToolCallbacks for an owner's enabled Plugins (builtin + remote MCP).
 */
public interface PluginToolGateway {

    List<ToolCallback> resolveEnabledToolCallbacks(String ownerKey);

    /**
     * Probe a remote Streamable HTTP MCP endpoint and return discovered tool names.
     */
    List<String> listRemoteToolNames(String endpoint, String authToken);

    boolean pingRemote(String endpoint, String authToken);
}
