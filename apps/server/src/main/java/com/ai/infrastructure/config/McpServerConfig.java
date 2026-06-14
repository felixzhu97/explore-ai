package com.ai.infrastructure.config;

import com.ai.application.port.ToolRegistryPort;
import com.ai.infrastructure.adapter.mcp.McpToolCallbackAdapter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Bridges application-layer tool providers to the Spring AI 2.0 MCP server.
 *
 * <p>The {@link #mcpToolCallbacks(ToolRegistryPort)} bean produces a
 * {@link ToolCallbackProvider} that exposes all registered tools as MCP callbacks.
 * Spring AI's {@code ToolCallbackConverterAutoConfiguration} collects all
 * {@link ToolCallbackProvider} beans (via {@code ObjectProvider<List<ToolCallbackProvider>>})
 * and converts each to a {@link io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification}
 * using {@link org.springframework.ai.mcp.McpToolUtils#toSyncToolSpecification}.
 * Those specifications are then registered with the autoconfigured
 * {@code McpSyncServer}, making the tools available at the MCP JSON-RPC endpoints.
 *
 * <p>The MCP server exposes JSON-RPC over HTTP at {@code /mcp}:
 * <ul>
 *   <li>POST requests must include both {@code Accept: text/event-stream, application/json}</li>
 *   <li>The initial request must be an {@code "initialize"} JSON-RPC call</li>
 *   <li>Subsequent requests must include the {@code Mcp-Session-Id} header from the init response</li>
 * </ul>
 */
@Configuration
public class McpServerConfig {

    /**
     * Creates a {@link ToolCallbackProvider} that exposes all registered tools as MCP callbacks.
     *
     * <p>{@code @Bean} method parameters are autowired by Spring — no explicit
     * {@code @Autowired} is needed (and is prohibited on {@code @Bean} methods in Spring 7+).
     *
     * @param registry the single source of truth for all tool definitions and invocations
     * @return a provider exposing all registered tools as {@link ToolCallback} instances
     */
    @Bean
    public ToolCallbackProvider mcpToolCallbacks(ToolRegistryPort registry) {
        List<ToolCallback> callbacks = registry.listTools().stream()
                .<ToolCallback>map(def -> new McpToolCallbackAdapter(def, registry))
                .toList();

        return () -> callbacks.toArray(ToolCallback[]::new);
    }
}
