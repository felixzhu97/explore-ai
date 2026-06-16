package com.ai.infrastructure.adapter.tool.monitor;

import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.infrastructure.adapter.monitor.SystemInfoProvider;
import com.ai.infrastructure.adapter.tool.ToolProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Tool for retrieving system memory statistics.
 * Returns total, used, free memory, swap, and JVM heap/non-heap usage.
 */
@Component
public class GetMemoryTool implements ToolProvider {

    private final SystemInfoProvider systemInfoProvider;

    public GetMemoryTool(SystemInfoProvider systemInfoProvider) {
        this.systemInfoProvider = systemInfoProvider;
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.atomic(
            "get_memory",
            "Get system memory statistics including total, used, free memory, swap usage, and JVM heap/non-heap memory.",
            Map.of("type", "object", "properties", Map.of()),
            "monitor"
        );
    }

    @Override
    public ToolExecutor executor() {
        return this::execute;
    }

    public ToolResult execute(ToolInvocation invocation) {
        try {
            SystemInfoProvider.MemoryInfo mem = systemInfoProvider.memory();

            StringBuilder sb = new StringBuilder();
            sb.append("# Memory Information\n\n");
            sb.append("## System Memory\n\n");
            sb.append("| Property | Value |\n");
            sb.append("|---|---|\n");
            sb.append("| Total | ").append(formatBytes(mem.totalBytes())).append(" |\n");
            sb.append("| Used | ").append(formatBytes(mem.usedBytes())).append(" |\n");
            sb.append("| Free | ").append(formatBytes(mem.freeBytes())).append(" |\n");
            sb.append("| **Usage** | ").append(String.format("%.1f%%", mem.usedPercent())).append(" |\n\n");

            sb.append("## Swap Memory\n\n");
            sb.append("| Property | Value |\n");
            sb.append("|---|---|\n");
            sb.append("| Total | ").append(formatBytes(mem.swapTotalBytes())).append(" |\n");
            sb.append("| Used | ").append(formatBytes(mem.swapUsedBytes())).append(" |\n");
            sb.append("| Free | ").append(formatBytes(mem.swapFreeBytes())).append(" |\n\n");

            if (mem.jvm() != null) {
                SystemInfoProvider.JvmMemoryInfo jvm = mem.jvm();
                sb.append("## JVM Memory\n\n");
                sb.append("| Property | Value |\n");
                sb.append("|---|---|\n");
                sb.append("| Heap Used | ").append(formatBytes(jvm.heapUsedBytes())).append(" |\n");
                sb.append("| Heap Max | ").append(formatBytes(jvm.heapMaxBytes())).append(" |\n");
                sb.append("| Heap Usage | ").append(String.format("%.1f%%", jvm.heapUsedPercent())).append(" |\n");
                sb.append("| Non-Heap Used | ").append(formatBytes(jvm.nonHeapUsedBytes())).append(" |\n");
            }

            Map<String, Object> structured = Map.of(
                "totalBytes", mem.totalBytes(),
                "usedBytes", mem.usedBytes(),
                "freeBytes", mem.freeBytes(),
                "usedPercent", mem.usedPercent(),
                "swapTotalBytes", mem.swapTotalBytes(),
                "swapUsedBytes", mem.swapUsedBytes(),
                "swapFreeBytes", mem.swapFreeBytes(),
                "jvm", mem.jvm() != null ? Map.of(
                    "heapUsedBytes", mem.jvm().heapUsedBytes(),
                    "heapMaxBytes", mem.jvm().heapMaxBytes(),
                    "heapUsedPercent", mem.jvm().heapUsedPercent(),
                    "nonHeapUsedBytes", mem.jvm().nonHeapUsedBytes()
                ) : Map.of()
            );

            return ToolResult.success(sb.toString(), structured);
        } catch (Exception e) {
            return ToolResult.error("Failed to get memory information: " + e.getMessage());
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes >= 1_073_741_824) {
            return String.format("%.2f GB", bytes / (double) (1_073_741_824));
        } else if (bytes >= 1_048_576) {
            return String.format("%.2f MB", bytes / (double) (1_048_576));
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", bytes / (double) 1024);
        }
        return bytes + " B";
    }
}
