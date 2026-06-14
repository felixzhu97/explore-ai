package com.ai.infrastructure.adapter.tool.monitor;

import com.ai.application.tool.ToolDefinition;
import com.ai.application.tool.ToolExecutor;
import com.ai.application.tool.ToolInvocation;
import com.ai.application.tool.ToolResult;
import com.ai.infrastructure.adapter.monitor.SystemInfoProvider;
import com.ai.infrastructure.adapter.tool.ToolProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool for retrieving JVM runtime statistics.
 * Returns heap/non-heap memory, GC statistics, threads, classes, uptime, and JVM arguments.
 */
@Component
public class GetJvmTool implements ToolProvider {

    private final SystemInfoProvider systemInfoProvider;

    public GetJvmTool(SystemInfoProvider systemInfoProvider) {
        this.systemInfoProvider = systemInfoProvider;
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.atomic(
            "get_jvm",
            "Get JVM runtime statistics including heap/non-heap memory usage, garbage collector statistics, thread counts, loaded classes, uptime, and JVM arguments.",
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
            SystemInfoProvider.JvmInfo jvm = systemInfoProvider.jvm();

            StringBuilder sb = new StringBuilder();
            sb.append("# JVM Information\n\n");

            // Memory section
            sb.append("## Memory\n\n");
            sb.append("| Property | Value |\n");
            sb.append("|---|---|\n");
            sb.append("| Heap Used | ").append(formatBytes(jvm.heapUsedBytes())).append(" |\n");
            sb.append("| Heap Max | ").append(formatBytes(jvm.heapMaxBytes())).append(" |\n");
            sb.append("| Heap Usage | ").append(String.format("%.1f%%", jvm.heapUsedPercent())).append(" |\n");
            sb.append("| Non-Heap Used | ").append(formatBytes(jvm.nonHeapUsedBytes())).append(" |\n\n");

            // Threads section
            sb.append("## Threads\n\n");
            sb.append("| Property | Value |\n");
            sb.append("|---|---|\n");
            sb.append("| Total Threads | ").append(jvm.threads()).append(" |\n");
            sb.append("| Daemon Threads | ").append(jvm.daemonThreads()).append(" |\n\n");

            // Classes section
            sb.append("## Classes\n\n");
            sb.append("| Property | Value |\n");
            sb.append("|---|---|\n");
            sb.append("| Classes Loaded | ").append(jvm.classesLoaded()).append(" |\n\n");

            // Uptime section
            sb.append("## Runtime\n\n");
            sb.append("| Property | Value |\n");
            sb.append("|---|---|\n");
            sb.append("| Uptime | ").append(formatUptime(jvm.uptimeSeconds())).append(" |\n\n");

            // GC section
            if (jvm.gcCollectors() != null && !jvm.gcCollectors().isEmpty()) {
                sb.append("## Garbage Collectors\n\n");
                sb.append("| Name | Collections | Collection Time |\n");
                sb.append("|---|---|---|\n");
                for (SystemInfoProvider.GarbageCollectorInfo gc : jvm.gcCollectors()) {
                    sb.append("| ").append(gc.name());
                    sb.append(" | ").append(gc.collectionCount());
                    sb.append(" | ").append(formatMs(gc.collectionTimeMs()));
                    sb.append(" |\n");
                }
                sb.append("\n");
            }

            // JVM Args section
            if (jvm.jvmArgs() != null && jvm.jvmArgs().length > 0) {
                sb.append("## JVM Arguments\n\n");
                sb.append("```\n");
                for (String arg : jvm.jvmArgs()) {
                    sb.append(arg).append("\n");
                }
                sb.append("```\n");
            }

            Map<String, Object> structured = Map.of(
                "heapUsedBytes", jvm.heapUsedBytes(),
                "heapMaxBytes", jvm.heapMaxBytes(),
                "heapUsedPercent", jvm.heapUsedPercent(),
                "nonHeapUsedBytes", jvm.nonHeapUsedBytes(),
                "threads", jvm.threads(),
                "daemonThreads", jvm.daemonThreads(),
                "classesLoaded", jvm.classesLoaded(),
                "uptimeSeconds", jvm.uptimeSeconds(),
                "gcCollectors", jvm.gcCollectors().stream()
                    .map(gc -> Map.of(
                        "name", gc.name(),
                        "collectionCount", gc.collectionCount(),
                        "collectionTimeMs", gc.collectionTimeMs()
                    ))
                    .toList(),
                "jvmArgs", List.of(jvm.jvmArgs())
            );

            return ToolResult.success(sb.toString(), structured);
        } catch (Exception e) {
            return ToolResult.error("Failed to get JVM information: " + e.getMessage());
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

    private static String formatUptime(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        } else if (seconds < 3600) {
            return String.format("%.1f minutes", seconds / 60.0);
        } else if (seconds < 86400) {
            return String.format("%.1f hours", seconds / 3600.0);
        } else {
            return String.format("%.1f days", seconds / 86400.0);
        }
    }

    private static String formatMs(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        } else {
            return String.format("%.2f s", ms / 1000.0);
        }
    }
}
