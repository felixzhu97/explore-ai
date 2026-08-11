package com.ai.plugin.infrastructure.mcp;

import com.ai.mcp.infrastructure.server.AiMcpServerService;
import com.ai.plugin.domain.model.PluginInstallation;
import com.ai.plugin.domain.repository.PluginInstallationRepository;
import com.ai.plugin.domain.repository.PluginToolGateway;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SpringAiPluginToolGateway implements PluginToolGateway {

    private static final Logger log = LoggerFactory.getLogger(SpringAiPluginToolGateway.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration INIT_TIMEOUT = Duration.ofSeconds(15);

    private final PluginInstallationRepository installationRepository;
    private final AiMcpServerService builtinServer;
    private final Map<String, CachedClient> clientCache = new ConcurrentHashMap<>();

    public SpringAiPluginToolGateway(
            PluginInstallationRepository installationRepository,
            AiMcpServerService builtinServer) {
        this.installationRepository = installationRepository;
        this.builtinServer = builtinServer;
    }

    @Override
    public List<ToolCallback> resolveEnabledToolCallbacks(String ownerKey) {
        List<ToolCallback> callbacks = new ArrayList<>();
        for (PluginInstallation installation : installationRepository.findAllByOwnerKey(ownerKey)) {
            if (!installation.isEnabled()) {
                continue;
            }
            if (installation.isBuiltin()) {
                callbacks.addAll(builtinCallbacks());
                continue;
            }
            if (installation.getEndpoint() == null || installation.getEndpoint().isBlank()) {
                continue;
            }
            try {
                callbacks.addAll(remoteCallbacks(installation));
            } catch (RuntimeException ex) {
                log.warn(
                        "Plugin {} tools unavailable for owner {}: {}",
                        installation.getDefinitionId(),
                        ownerKey,
                        ex.getMessage());
                installation.markHealth(com.ai.plugin.domain.vo.PluginHealthStatus.UNHEALTHY);
                installationRepository.save(installation);
            }
        }
        return callbacks;
    }

    @Override
    public List<String> listRemoteToolNames(String endpoint, String authToken) {
        try (McpSyncClient client = openClient(endpoint, authToken)) {
            client.initialize();
            McpSchema.ListToolsResult tools = client.listTools();
            if (tools == null || tools.tools() == null) {
                return List.of();
            }
            return tools.tools().stream().map(McpSchema.Tool::name).toList();
        }
    }

    @Override
    public boolean pingRemote(String endpoint, String authToken) {
        try {
            listRemoteToolNames(endpoint, authToken);
            return true;
        } catch (RuntimeException ex) {
            log.debug("Remote MCP ping failed for {}: {}", endpoint, ex.getMessage());
            return false;
        }
    }

    private List<ToolCallback> remoteCallbacks(PluginInstallation installation) {
        String cacheKey = installation.getId().value() + "|" + installation.getUpdatedAt().toEpochMilli();
        CachedClient cached = clientCache.compute(cacheKey, (key, existing) -> {
            if (existing != null) {
                return existing;
            }
            McpSyncClient client = openClient(installation.getEndpoint(), installation.getAuthToken());
            client.initialize();
            ToolCallback[] tools = SyncMcpToolCallbackProvider.builder()
                    .mcpClients(client)
                    .build()
                    .getToolCallbacks();
            installation.markHealth(com.ai.plugin.domain.vo.PluginHealthStatus.HEALTHY);
            installationRepository.save(installation);
            return new CachedClient(client, List.of(tools == null ? new ToolCallback[0] : tools));
        });
        return cached.callbacks();
    }

    private McpSyncClient openClient(String endpointUrl, String authToken) {
        URI uri = URI.create(endpointUrl.trim());
        String baseUrl = uri.getScheme() + "://" + uri.getAuthority();
        String path = uri.getRawPath();
        if (path == null || path.isBlank()) {
            path = "/mcp";
        }
        HttpClientStreamableHttpTransport.Builder transportBuilder = HttpClientStreamableHttpTransport
                .builder(baseUrl)
                .endpoint(path)
                .connectTimeout(INIT_TIMEOUT);
        if (authToken != null && !authToken.isBlank()) {
            String token = authToken;
            McpSyncHttpClientRequestCustomizer customizer =
                    (builder, method, endpoint, body, context) -> builder.header("Authorization", bearer(token));
            transportBuilder.httpRequestCustomizer(customizer);
        }
        return McpClient.sync(transportBuilder.build())
                .requestTimeout(REQUEST_TIMEOUT)
                .initializationTimeout(INIT_TIMEOUT)
                .clientInfo(new McpSchema.Implementation("explore-ai-plugin", "1.0.0"))
                .build();
    }

    private static String bearer(String token) {
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return token;
        }
        return "Bearer " + token;
    }

    private List<ToolCallback> builtinCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();
        callbacks.add(FunctionToolCallback.builder("get_weather", (Map<String, Object> input) ->
                        builtinServer.getWeather(stringArg(input, "city")))
                .description("Get current weather information for a specified city")
                .inputType(Map.class)
                .build());
        callbacks.add(FunctionToolCallback.builder("get_forecast", (Map<String, Object> input) ->
                        builtinServer.getForecast(
                                stringArg(input, "city"),
                                intArg(input, "days")))
                .description("Get weather forecast for a specified city")
                .inputType(Map.class)
                .build());
        callbacks.add(FunctionToolCallback.builder("search_knowledge_base", (Map<String, Object> input) ->
                        builtinServer.searchKnowledgeBase(
                                stringArg(input, "query"),
                                stringArg(input, "docIds")))
                .description("Search documents in the knowledge base using semantic search")
                .inputType(Map.class)
                .build());
        callbacks.add(FunctionToolCallback.builder("list_documents", (Map<String, Object> input) ->
                        builtinServer.listDocuments())
                .description("List all documents available in the knowledge base")
                .inputType(Map.class)
                .build());
        return callbacks;
    }

    private static String stringArg(Map<String, Object> input, String key) {
        if (input == null || input.get(key) == null) {
            return null;
        }
        return String.valueOf(input.get(key));
    }

    private static Integer intArg(Map<String, Object> input, String key) {
        if (input == null || input.get(key) == null) {
            return null;
        }
        Object value = input.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record CachedClient(McpSyncClient client, List<ToolCallback> callbacks) {
    }
}
