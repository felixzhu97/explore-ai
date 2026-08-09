package com.ai.mcp.infrastructure.config;

import io.modelcontextprotocol.client.McpClient;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("McpClientResilienceConfig")
class McpClientResilienceConfigTest {
    @Test
    @DisplayName("should apply request and initialization timeouts")
    void shouldApplyRequestAndInitializationTimeouts() {
        McpClientResilienceConfig config = new McpClientResilienceConfig();
        Duration timeout = Duration.ofSeconds(15);
        var customizer = config.mcpClientTimeoutCustomizer(timeout);
        McpClient.SyncSpec spec = mock(McpClient.SyncSpec.class);
        when(spec.requestTimeout(any())).thenReturn(spec);
        when(spec.initializationTimeout(any())).thenReturn(spec);
        customizer.customize("remote", spec);
        verify(spec).requestTimeout(timeout);
        verify(spec).initializationTimeout(timeout);
        assertThat(customizer).isNotNull();
    }
}
