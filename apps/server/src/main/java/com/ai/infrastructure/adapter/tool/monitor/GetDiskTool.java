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
 * Tool for retrieving disk/filesystem statistics.
 * Returns per-mount statistics including total, used, free space and inode usage.
 */
@Component
public class GetDiskTool implements ToolProvider {

    private final SystemInfoProvider systemInfoProvider;

    public GetDiskTool(SystemInfoProvider systemInfoProvider) {
        this.systemInfoProvider = systemInfoProvider;
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.atomic(
            "get_disk",
            "Get disk/filesystem statistics. Returns per-mount information including total, used, free space and usage percentage. Optionally filter by path.",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "path", Map.of("type", "string", "description", "Optional mount point path to filter (e.g., '/' or '/home')")
                ),
                "required", List.of()
            ),
            "monitor"
        );
    }

    @Override
    public ToolExecutor executor() {
        return this::execute;
    }

    public ToolResult execute(ToolInvocation invocation) {
        try {
            String targetPath = invocation.getArg("path", "/");

            List<SystemInfoProvider.DiskInfo> allDisks = systemInfoProvider.disks();

            // Filter by path if specified
            List<SystemInfoProvider.DiskInfo> disks;
            if (targetPath != null && !targetPath.isEmpty() && !targetPath.equals("/")) {
                final String target = targetPath;
                disks = allDisks.stream()
                    .filter(d -> d.mountPoint().startsWith(target))
                    .toList();
                if (disks.isEmpty()) {
                    // Try exact match
                    disks = allDisks.stream()
                        .filter(d -> d.mountPoint().equals(target))
                        .toList();
                }
            } else {
                disks = allDisks;
            }

            if (disks.isEmpty()) {
                return ToolResult.error("No disks found for path: " + targetPath);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("# Disk Information\n\n");
            sb.append("| Mount Point | Name | Type | Total | Used | Free | Usage |\n");
            sb.append("|---|---|---|---|---|---|---|\n");

            for (SystemInfoProvider.DiskInfo disk : disks) {
                sb.append("| ").append(disk.mountPoint());
                sb.append(" | ").append(disk.name());
                sb.append(" | ").append(disk.type());
                sb.append(" | ").append(formatBytes(disk.totalBytes()));
                sb.append(" | ").append(formatBytes(disk.usedBytes()));
                sb.append(" | ").append(formatBytes(disk.freeBytes()));
                sb.append(" | ").append(String.format("%.1f%%", disk.usedPercent()));
                sb.append(" |\n");
            }

            List<Map<String, Object>> structuredDisks = disks.stream()
                .map(d -> Map.<String, Object>of(
                    "mountPoint", d.mountPoint(),
                    "name", d.name(),
                    "type", d.type(),
                    "totalBytes", d.totalBytes(),
                    "usedBytes", d.usedBytes(),
                    "freeBytes", d.freeBytes(),
                    "usedPercent", d.usedPercent(),
                    "totalInodes", d.totalInodes(),
                    "usedInodes", d.usedInodes()
                ))
                .toList();

            Map<String, Object> structured = Map.of(
                "path", targetPath,
                "disks", structuredDisks
            );

            return ToolResult.success(sb.toString(), structured);
        } catch (Exception e) {
            return ToolResult.error("Failed to get disk information: " + e.getMessage());
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes >= 1_099_511_627_776L) {
            return String.format("%.2f TB", bytes / (double) (1_099_511_627_776L));
        } else if (bytes >= 1_073_741_824) {
            return String.format("%.2f GB", bytes / (double) (1_073_741_824));
        } else if (bytes >= 1_048_576) {
            return String.format("%.2f MB", bytes / (double) (1_048_576));
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", bytes / (double) 1024);
        }
        return bytes + " B";
    }
}
