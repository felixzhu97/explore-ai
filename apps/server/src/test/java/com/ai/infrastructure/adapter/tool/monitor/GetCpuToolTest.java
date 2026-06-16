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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GetCpuTool.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetCpuTool")
class GetCpuToolTest {

    @Mock
    private SystemInfoProvider systemInfoProvider;

    private GetCpuTool getCpuTool;

    @BeforeEach
    void setUp() {
        getCpuTool = new GetCpuTool(systemInfoProvider);
    }

    @Nested
    @DisplayName("definition")
    class DefinitionTests {

        @Test
        @DisplayName("should return correct tool definition")
        void shouldReturnCorrectToolDefinition() {
            ToolDefinition definition = getCpuTool.definition();

            assertThat(definition.name()).isEqualTo("get_cpu");
            assertThat(definition.category()).isEqualTo("monitor");
            assertThat(definition.composite()).isFalse();
            assertThat(definition.description()).isNotEmpty();
            assertThat(definition.inputSchema()).isNotNull();
        }

        @Test
        @DisplayName("should have empty input schema")
        void shouldHaveEmptyInputSchema() {
            ToolDefinition definition = getCpuTool.definition();
            Map<String, Object> schema = definition.inputSchema();

            assertThat(schema).containsEntry("type", "object");
            assertThat(schema).containsKey("properties");
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            assertThat(properties).isEmpty();
        }
    }

    @Nested
    @DisplayName("execute")
    class ExecuteTests {

        @Test
        @DisplayName("should return CPU info when successful")
        void shouldReturnCpuInfoWhenSuccessful() {
            // Arrange
            SystemInfoProvider.CpuInfo cpuInfo = new SystemInfoProvider.CpuInfo(
                "Intel Core i7",
                4,
                8,
                25.0,
                10.0,
                65.0,
                new double[]{10.0, 20.0, 15.0, 30.0}
            );
            when(systemInfoProvider.cpuUsagePercent()).thenReturn(cpuInfo);

            ToolInvocation invocation = new ToolInvocation("get_cpu", Map.of());

            // Act
            ToolResult result = getCpuTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("CPU Information");
            assertThat(result.content()).contains("Intel Core i7");
            assertThat(result.content()).contains("Physical Cores");
            assertThat(result.content()).contains("Logical Cores");
            assertThat(result.content()).contains("Per-Core Load");
        }

        @Test
        @DisplayName("should include per-core load when cores present")
        void shouldIncludePerCoreLoadWhenCoresPresent() {
            // Arrange
            double[] perCoreLoads = {10.5, 20.5, 15.0, 30.0, 5.0, 12.0, 8.0, 25.0};
            SystemInfoProvider.CpuInfo cpuInfo = new SystemInfoProvider.CpuInfo(
                "AMD Ryzen 9",
                8,
                16,
                30.0,
                20.0,
                50.0,
                perCoreLoads
            );
            when(systemInfoProvider.cpuUsagePercent()).thenReturn(cpuInfo);

            ToolInvocation invocation = new ToolInvocation("get_cpu", Map.of());

            // Act
            ToolResult result = getCpuTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("Per-Core Load");
            assertThat(result.content()).contains("Core 0");
            assertThat(result.content()).contains("Core 7");
        }

        @Test
        @DisplayName("should return structured data")
        void shouldReturnStructuredData() {
            // Arrange
            double[] perCoreLoads = {50.0};
            SystemInfoProvider.CpuInfo cpuInfo = new SystemInfoProvider.CpuInfo(
                "Intel Xeon",
                2,
                4,
                20.0,
                30.0,
                50.0,
                perCoreLoads
            );
            when(systemInfoProvider.cpuUsagePercent()).thenReturn(cpuInfo);

            ToolInvocation invocation = new ToolInvocation("get_cpu", Map.of());

            // Act
            ToolResult result = getCpuTool.execute(invocation);

            // Assert
            assertThat(result.structured()).isNotNull();
            assertThat(result.structured()).containsEntry("modelName", "Intel Xeon");
            assertThat(result.structured()).containsEntry("physicalCores", 2);
            assertThat(result.structured()).containsEntry("logicalCores", 4);
        }

        @Test
        @DisplayName("should return error when provider throws")
        void shouldReturnErrorWhenProviderThrows() {
            // Arrange
            when(systemInfoProvider.cpuUsagePercent()).thenThrow(new RuntimeException("CPU info unavailable"));

            ToolInvocation invocation = new ToolInvocation("get_cpu", Map.of());

            // Act
            ToolResult result = getCpuTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("Failed to get CPU information");
        }
    }
}
