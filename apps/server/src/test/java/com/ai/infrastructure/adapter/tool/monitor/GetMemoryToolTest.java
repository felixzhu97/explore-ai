package com.ai.infrastructure.adapter.tool.monitor;

import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.infrastructure.adapter.monitor.SystemInfoProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GetMemoryTool.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetMemoryTool")
class GetMemoryToolTest {

    @Mock
    private SystemInfoProvider systemInfoProvider;

    private GetMemoryTool getMemoryTool;

    @BeforeEach
    void setUp() {
        getMemoryTool = new GetMemoryTool(systemInfoProvider);
    }

    @Nested
    @DisplayName("definition")
    class DefinitionTests {

        @Test
        @DisplayName("should return correct tool definition")
        void shouldReturnCorrectToolDefinition() {
            ToolDefinition definition = getMemoryTool.definition();

            assertThat(definition.name()).isEqualTo("get_memory");
            assertThat(definition.category()).isEqualTo("monitor");
            assertThat(definition.composite()).isFalse();
            assertThat(definition.description()).isNotEmpty();
        }

        @Test
        @DisplayName("should have empty input schema")
        void shouldHaveEmptyInputSchema() {
            ToolDefinition definition = getMemoryTool.definition();
            Map<String, Object> schema = definition.inputSchema();

            assertThat(schema).containsEntry("type", "object");
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            assertThat(properties).isEmpty();
        }
    }

    @Nested
    @DisplayName("execute")
    class ExecuteTests {

        @Test
        @DisplayName("should return memory info when successful")
        void shouldReturnMemoryInfoWhenSuccessful() {
            // Arrange
            SystemInfoProvider.JvmMemoryInfo jvmInfo = new SystemInfoProvider.JvmMemoryInfo(
                536870912L,  // 512 MB
                1073741824L, // 1 GB
                50.0,
                67108864L    // 64 MB
            );
            SystemInfoProvider.MemoryInfo memInfo = new SystemInfoProvider.MemoryInfo(
                17179869184L,  // 16 GB
                8589934592L,   // 8 GB
                8589934592L,    // 8 GB
                4294967296L,    // 4 GB
                1073741824L,    // 1 GB
                3221225472L,    // 3 GB
                50.0,
                jvmInfo
            );
            when(systemInfoProvider.memory()).thenReturn(memInfo);

            ToolInvocation invocation = new ToolInvocation("get_memory", Map.of());

            // Act
            ToolResult result = getMemoryTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("Memory Information");
            assertThat(result.content()).contains("System Memory");
            assertThat(result.content()).contains("Swap Memory");
            assertThat(result.content()).contains("JVM Memory");
        }

        @Test
        @DisplayName("should include JVM memory details")
        void shouldIncludeJvmMemoryDetails() {
            // Arrange
            SystemInfoProvider.JvmMemoryInfo jvmInfo = new SystemInfoProvider.JvmMemoryInfo(
                2147483648L,  // 2 GB
                4294967296L,  // 4 GB
                50.0,
                134217728L    // 128 MB
            );
            SystemInfoProvider.MemoryInfo memInfo = new SystemInfoProvider.MemoryInfo(
                8589934592L,  // 8 GB
                4294967296L,  // 4 GB
                4294967296L,  // 4 GB
                0L, 0L, 0L, 50.0, jvmInfo
            );
            when(systemInfoProvider.memory()).thenReturn(memInfo);

            ToolInvocation invocation = new ToolInvocation("get_memory", Map.of());

            // Act
            ToolResult result = getMemoryTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("Heap Used");
            assertThat(result.content()).contains("Heap Max");
            assertThat(result.content()).contains("Non-Heap Used");
        }

        @Test
        @DisplayName("should return structured data")
        void shouldReturnStructuredData() {
            // Arrange
            SystemInfoProvider.MemoryInfo memInfo = new SystemInfoProvider.MemoryInfo(
                17179869184L, 8589934592L, 8589934592L, 0L, 0L, 0L, 50.0, null
            );
            when(systemInfoProvider.memory()).thenReturn(memInfo);

            ToolInvocation invocation = new ToolInvocation("get_memory", Map.of());

            // Act
            ToolResult result = getMemoryTool.execute(invocation);

            // Assert
            assertThat(result.structured()).isNotNull();
            assertThat(result.structured()).containsEntry("totalBytes", 17179869184L);
            assertThat(result.structured()).containsEntry("usedBytes", 8589934592L);
            assertThat(result.structured()).containsEntry("usedPercent", 50.0);
        }

        @Test
        @DisplayName("should return error when provider throws")
        void shouldReturnErrorWhenProviderThrows() {
            // Arrange
            when(systemInfoProvider.memory()).thenThrow(new RuntimeException("Memory info unavailable"));

            ToolInvocation invocation = new ToolInvocation("get_memory", Map.of());

            // Act
            ToolResult result = getMemoryTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("Failed to get memory information");
        }
    }
}
