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
 * Tool for retrieving system load average statistics.
 * Returns 1/5/15-minute load averages with context on available processors.
 */
@Component
public class GetLoadTool implements ToolProvider {

    private final SystemInfoProvider systemInfoProvider;

    public GetLoadTool(SystemInfoProvider systemInfoProvider) {
        this.systemInfoProvider = systemInfoProvider;
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.atomic(
            "get_load",
            "Get system load averages for 1, 5, and 15 minute periods. Load average indicates the average number of runnable processes waiting for CPU time.",
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
            SystemInfoProvider.LoadInfo load = systemInfoProvider.loadAverage();

            StringBuilder sb = new StringBuilder();
            sb.append("# System Load Average\n\n");
            sb.append("| Period | Load Average | Context |\n");
            sb.append("|---|---|---|\n");
            sb.append("| 1 Minute | ").append(String.format("%.2f", load.load1Min())).append(" | ");
            sb.append(getLoadContext(load.load1Min(), load.availableProcessors())).append(" |\n");
            sb.append("| 5 Minutes | ").append(String.format("%.2f", load.load5Min())).append(" | ");
            sb.append(getLoadContext(load.load5Min(), load.availableProcessors())).append(" |\n");
            sb.append("| 15 Minutes | ").append(String.format("%.2f", load.load15Min())).append(" | ");
            sb.append(getLoadContext(load.load15Min(), load.availableProcessors())).append(" |\n\n");
            sb.append("**Available Processors (Cores):** ").append(load.availableProcessors()).append("\n\n");

            if (load.load1Min() > load.availableProcessors()) {
                sb.append("> **Note:** 1-minute load exceeds available processors, indicating CPU contention.\n");
            }

            Map<String, Object> structured = Map.of(
                "load1Min", load.load1Min(),
                "load5Min", load.load5Min(),
                "load15Min", load.load15Min(),
                "availableProcessors", load.availableProcessors()
            );

            return ToolResult.success(sb.toString(), structured);
        } catch (Exception e) {
            return ToolResult.error("Failed to get load average: " + e.getMessage());
        }
    }

    private static String getLoadContext(double load, int processors) {
        double ratio = load / processors;
        if (ratio < 0.5) {
            return "Low load";
        } else if (ratio < 0.8) {
            return "Moderate load";
        } else if (ratio < 1.0) {
            return "Normal load";
        } else if (ratio < 1.5) {
            return "High load";
        } else {
            return "Very high load";
        }
    }
}
