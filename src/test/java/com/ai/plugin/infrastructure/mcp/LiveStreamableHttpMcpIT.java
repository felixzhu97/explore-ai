package com.ai.plugin.infrastructure.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live smoke against a running bootRun on :9000 (skipped if server is down).
 */
@DisplayName("Live Streamable HTTP MCP")
class LiveStreamableHttpMcpIT {

    private static final String BASE = "http://127.0.0.1:9000";
    private static final String ENDPOINT = "/mcp";

    @Test
    @DisplayName("should call get_weather over Streamable HTTP when server is up")
    void shouldCallGetWeatherOverStreamableHttpWhenServerIsUp() {
        Assumptions.assumeTrue(serverUp(), "bootRun on :9000 is required");

        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder(BASE)
                .endpoint(ENDPOINT)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        try (McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(20))
                .initializationTimeout(Duration.ofSeconds(15))
                .clientInfo(new McpSchema.Implementation("explore-ai-live-it", "1.0.0"))
                .build()) {
            client.initialize();
            McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest(
                    "get_weather",
                    Map.of("city", "北京")));

            assertThat(result).isNotNull();
            assertThat(String.valueOf(result)).contains("北京");
            System.out.println("MCP tools/call result: " + result);
        }
    }

    private static boolean serverUp() {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(BASE + "/actuator/health")
                    .toURL()
                    .openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setRequestMethod("GET");
            return connection.getResponseCode() == 200;
        } catch (Exception ex) {
            return false;
        }
    }
}
