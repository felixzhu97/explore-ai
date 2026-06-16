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
 * Unit tests for GetDiskTool.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetDiskTool")
class GetDiskToolTest {

    @Mock
    private SystemInfoProvider systemInfoProvider;

    private GetDiskTool getDiskTool;

    @BeforeEach
    void setUp() {
        getDiskTool = new GetDiskTool(systemInfoProvider);
    }

    @Nested
    @DisplayName("definition")
    class DefinitionTests {

        @Test
        @DisplayName("should return correct tool definition")
        void shouldReturnCorrectToolDefinition() {
            ToolDefinition definition = getDiskTool.definition();

            assertThat(definition.name()).isEqualTo("get_disk");
            assertThat(definition.category()).isEqualTo("monitor");
            assertThat(definition.composite()).isFalse();
            assertThat(definition.description()).isNotEmpty();
        }

        @Test
        @DisplayName("should have optional path parameter")
        void shouldHaveOptionalPathParameter() {
            ToolDefinition definition = getDiskTool.definition();
            Map<String, Object> schema = definition.inputSchema();

            assertThat(schema).containsEntry("type", "object");
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
            assertThat(properties).containsKey("path");
        }
    }

    @Nested
    @DisplayName("execute")
    class ExecuteTests {

        @Test
        @DisplayName("should return all disks when no path specified")
        void shouldReturnAllDisksWhenNoPathSpecified() {
            // Arrange
            List<SystemInfoProvider.DiskInfo> disks = List.of(
                new SystemInfoProvider.DiskInfo("/", " Macintosh HD", "apfs", 500000000000L, 250000000000L, 250000000000L, 50.0, 1000000L, 500000L),
                new SystemInfoProvider.DiskInfo("/home", "Data Volume", "ext4", 1000000000000L, 300000000000L, 700000000000L, 30.0, 2000000L, 1000000L)
            );
            when(systemInfoProvider.disks()).thenReturn(disks);

            ToolInvocation invocation = new ToolInvocation("get_disk", Map.of());

            // Act
            ToolResult result = getDiskTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("Disk Information");
            assertThat(result.content()).contains("/");
            assertThat(result.content()).contains("/home");
        }

        @Test
        @DisplayName("should filter disks by path")
        void shouldFilterDisksByPath() {
            // Arrange
            List<SystemInfoProvider.DiskInfo> disks = List.of(
                new SystemInfoProvider.DiskInfo("/", " Macintosh HD", "apfs", 500000000000L, 250000000000L, 250000000000L, 50.0, 1000000L, 500000L),
                new SystemInfoProvider.DiskInfo("/home/user", "Data Volume", "ext4", 1000000000000L, 300000000000L, 700000000000L, 30.0, 2000000L, 1000000L)
            );
            when(systemInfoProvider.disks()).thenReturn(disks);

            ToolInvocation invocation = new ToolInvocation("get_disk", Map.of("path", "/home"));

            // Act
            ToolResult result = getDiskTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isFalse();
            assertThat(result.content()).contains("/home/user");
            assertThat(result.content()).doesNotContain("Macintosh HD");
        }

        @Test
        @DisplayName("should return error when no disks found for path")
        void shouldReturnErrorWhenNoDisksFoundForPath() {
            // Arrange
            List<SystemInfoProvider.DiskInfo> disks = List.of(
                new SystemInfoProvider.DiskInfo("/", " Macintosh HD", "apfs", 500000000000L, 250000000000L, 250000000000L, 50.0, 1000000L, 500000L)
            );
            when(systemInfoProvider.disks()).thenReturn(disks);

            ToolInvocation invocation = new ToolInvocation("get_disk", Map.of("path", "/nonexistent"));

            // Act
            ToolResult result = getDiskTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("No disks found");
        }

        @Test
        @DisplayName("should return structured data")
        void shouldReturnStructuredData() {
            // Arrange
            List<SystemInfoProvider.DiskInfo> disks = List.of(
                new SystemInfoProvider.DiskInfo("/", " Macintosh HD", "apfs", 500000000000L, 250000000000L, 250000000000L, 50.0, 1000000L, 500000L)
            );
            when(systemInfoProvider.disks()).thenReturn(disks);

            ToolInvocation invocation = new ToolInvocation("get_disk", Map.of());

            // Act
            ToolResult result = getDiskTool.execute(invocation);

            // Assert
            assertThat(result.structured()).isNotNull();
            assertThat(result.structured()).containsKey("disks");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> diskList = (List<Map<String, Object>>) result.structured().get("disks");
            assertThat(diskList).hasSize(1);
            assertThat(diskList.get(0)).containsEntry("mountPoint", "/");
        }

        @Test
        @DisplayName("should return error when provider throws")
        void shouldReturnErrorWhenProviderThrows() {
            // Arrange
            when(systemInfoProvider.disks()).thenThrow(new RuntimeException("Disk info unavailable"));

            ToolInvocation invocation = new ToolInvocation("get_disk", Map.of());

            // Act
            ToolResult result = getDiskTool.execute(invocation);

            // Assert
            assertThat(result.isError()).isTrue();
            assertThat(result.content()).contains("Failed to get disk information");
        }
    }
}
