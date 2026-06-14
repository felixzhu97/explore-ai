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
 * Unit tests for GetJvmTool.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetJvmTool")
class GetJvmToolTest {

    @Mock
    private SystemInfoProvider systemInfoProvider;

    private GetJvmTool getJvmTool;

    @BeforeEach
    void setUp() {
        getJvmTool = new GetJvmTool(systemInfoProvider);
    }

    @Nested
    @DisplayName("definition")
    class DefinitionTests {

        @Test
        @DisplayName("should return correct tool definition")
        void shouldReturnCorrectToolDefinition() {
            ToolDefinition definition = getJvmTool.definition();

            assertThat(definition.name()).isEqualTo("get_jvm");
            assertThat(definition.category()).isEqualTo("monitor");
            assertThat(definition.composite()).isFalse();
            assertThat(definition.description()).isNotEmpty();
        }

        @Test
        @DisplayName("should have empty input schema")
        void shouldHaveEmptyInputSchema() {
            ToolDefinition definition = getJvmTool.definition();
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
        @DisplayName("should return JVM info when successful")
        void shouldReturnJvmInfoWhenSuccessful() {
            // Arrange
            SystemInfoProvider.JvmInfo jvmInfo = createJvmInfo();
            when(systemInfoProvider.jvm()).thenReturn(jvmInfo);

            ToolInvocation invocation = new ToolInvocation("get_jvm", Map.of());

            // Act
            ToolResult result = getJvmTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("JVM Information");
            assertThat(result.content()).contains("Memory");
            assertThat(result.content()).contains("Threads");
            assertThat(result.content()).contains("Classes");
            assertThat(result.content()).contains("Runtime");
        }

        @Test
        @DisplayName("should include garbage collector information")
        void shouldIncludeGarbageCollectorInformation() {
            // Arrange
            SystemInfoProvider.JvmInfo jvmInfo = createJvmInfo();
            when(systemInfoProvider.jvm()).thenReturn(jvmInfo);

            ToolInvocation invocation = new ToolInvocation("get_jvm", Map.of());

            // Act
            ToolResult result = getJvmTool.execute(invocation);

            // Assert
            assertThat(result.content()).contains("Garbage Collectors");
            assertThat(result.content()).contains("G1 Young Generation");
            assertThat(result.content()).contains("G1 Old Generation");
        }

        @Test
        @DisplayName("should include JVM arguments")
        void shouldIncludeJvmArguments() {
            // Arrange
            SystemInfoProvider.JvmInfo jvmInfo = createJvmInfo();
            when(systemInfoProvider.jvm()).thenReturn(jvmInfo);

            ToolInvocation invocation = new ToolInvocation("get_jvm", Map.of());

            // Act
            ToolResult result = getJvmTool.execute(invocation);

            // Assert
            assertThat(result.content()).contains("JVM Arguments");
            assertThat(result.content()).contains("-Xmx4g");
        }

        @Test
        @DisplayName("should return structured data")
        void shouldReturnStructuredData() {
            // Arrange
            SystemInfoProvider.JvmInfo jvmInfo = createJvmInfo();
            when(systemInfoProvider.jvm()).thenReturn(jvmInfo);

            ToolInvocation invocation = new ToolInvocation("get_jvm", Map.of());

            // Act
            ToolResult result = getJvmTool.execute(invocation);

            // Assert
            assertThat(result.structured()).isNotNull();
            assertThat(result.structured()).containsEntry("heapUsedBytes", 2147483648L);
            assertThat(result.structured()).containsEntry("heapMaxBytes", 4294967296L);
            assertThat(result.structured()).containsEntry("threads", 50);
            assertThat(result.structured()).containsEntry("daemonThreads", 25);
            assertThat(result.structured()).containsEntry("classesLoaded", 15000L);
        }

        @Test
        @DisplayName("should return error when provider throws")
        void shouldReturnErrorWhenProviderThrows() {
            // Arrange
            when(systemInfoProvider.jvm()).thenThrow(new RuntimeException("JVM info unavailable"));

            ToolInvocation invocation = new ToolInvocation("get_jvm", Map.of());

            // Act
            ToolResult result = getJvmTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("Failed to get JVM information");
        }

        private SystemInfoProvider.JvmInfo createJvmInfo() {
            List<SystemInfoProvider.GarbageCollectorInfo> gcInfos = List.of(
                new SystemInfoProvider.GarbageCollectorInfo("G1 Young Generation", 5000L, 125000L),
                new SystemInfoProvider.GarbageCollectorInfo("G1 Old Generation", 100L, 50000L)
            );
            return new SystemInfoProvider.JvmInfo(
                2147483648L,  // heapUsed
                4294967296L,  // heapMax
                50.0,         // heapPercent
                134217728L,   // nonHeap
                50,           // threads
                25,           // daemonThreads
                15000L,       // classesLoaded
                86400L,       // uptime (1 day)
                gcInfos,
                new String[]{"-Xmx4g", "-Xms2g", "-XX:+UseG1GC"}
            );
        }
    }
}
