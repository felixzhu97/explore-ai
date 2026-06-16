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

@ExtendWith(MockitoExtension.class)
@DisplayName("GetLoadTool")
class GetLoadToolTest {

    @Mock
    private SystemInfoProvider systemInfoProvider;

    private GetLoadTool getLoadTool;

    @BeforeEach
    void setUp() {
        getLoadTool = new GetLoadTool(systemInfoProvider);
    }

    @Nested
    @DisplayName("definition")
    class DefinitionTests {

        @Test
        @DisplayName("should return correct tool definition")
        void shouldReturnCorrectToolDefinition() {
            ToolDefinition definition = getLoadTool.definition();

            assertThat(definition.name()).isEqualTo("get_load");
            assertThat(definition.category()).isEqualTo("monitor");
            assertThat(definition.composite()).isFalse();
        }

        @Test
        @DisplayName("should have empty input schema")
        void shouldHaveEmptyInputSchema() {
            ToolDefinition definition = getLoadTool.definition();
            Map<String, Object> schema = definition.inputSchema();

            assertThat(schema).containsEntry("type", "object");
        }
    }

    @Nested
    @DisplayName("execute")
    class ExecuteTests {

        @Test
        @DisplayName("should return load average when successful")
        void shouldReturnLoadAverageWhenSuccessful() {
            SystemInfoProvider.LoadInfo loadInfo = new SystemInfoProvider.LoadInfo(
                2.5, 3.0, 4.0, 8
            );
            when(systemInfoProvider.loadAverage()).thenReturn(loadInfo);

            ToolResult result = getLoadTool.execute(new ToolInvocation("get_load", Map.of()));

            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("2.50");
            assertThat(result.content()).contains("3.00");
            assertThat(result.content()).contains("4.00");
        }

        @Test
        @DisplayName("should include available processors")
        void shouldIncludeAvailableProcessors() {
            SystemInfoProvider.LoadInfo loadInfo = new SystemInfoProvider.LoadInfo(
                1.0, 1.5, 2.0, 16
            );
            when(systemInfoProvider.loadAverage()).thenReturn(loadInfo);

            ToolResult result = getLoadTool.execute(new ToolInvocation("get_load", Map.of()));

            assertThat(result.content()).contains("16");
        }

        @Test
        @DisplayName("should include warning when load exceeds processors")
        void shouldIncludeWarningWhenLoadExceedsProcessors() {
            SystemInfoProvider.LoadInfo loadInfo = new SystemInfoProvider.LoadInfo(
                10.0, 8.0, 6.0, 4
            );
            when(systemInfoProvider.loadAverage()).thenReturn(loadInfo);

            ToolResult result = getLoadTool.execute(new ToolInvocation("get_load", Map.of()));

            assertThat(result.content()).contains("CPU contention");
        }

        @Test
        @DisplayName("should return structured data")
        void shouldReturnStructuredData() {
            SystemInfoProvider.LoadInfo loadInfo = new SystemInfoProvider.LoadInfo(
                2.5, 3.0, 4.0, 8
            );
            when(systemInfoProvider.loadAverage()).thenReturn(loadInfo);

            ToolResult result = getLoadTool.execute(new ToolInvocation("get_load", Map.of()));

            assertThat(result.structured()).isNotNull();
            assertThat(result.structured()).containsEntry("load1Min", 2.5);
            assertThat(result.structured()).containsEntry("load5Min", 3.0);
            assertThat(result.structured()).containsEntry("load15Min", 4.0);
        }

        @Test
        @DisplayName("should return error when provider throws")
        void shouldReturnErrorWhenProviderThrows() {
            when(systemInfoProvider.loadAverage()).thenThrow(new RuntimeException("Load info unavailable"));

            ToolResult result = getLoadTool.execute(new ToolInvocation("get_load", Map.of()));

            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("Failed to get load average");
        }
    }
}
