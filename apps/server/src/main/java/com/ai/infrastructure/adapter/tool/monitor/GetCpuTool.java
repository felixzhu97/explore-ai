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
 * Tool for retrieving CPU usage statistics.
 * Returns CPU model, physical/logical cores, overall usage, and per-core load.
 */
@Component
public class GetCpuTool implements ToolProvider {

    private final SystemInfoProvider systemInfoProvider;

    public GetCpuTool(SystemInfoProvider systemInfoProvider) {
        this.systemInfoProvider = systemInfoProvider;
    }

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.atomic(
            "get_cpu",
            "Get CPU usage statistics including model name, physical/logical cores, overall usage percentage, and per-core load.",
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
            SystemInfoProvider.CpuInfo cpu = systemInfoProvider.cpuUsagePercent();

            StringBuilder sb = new StringBuilder();
            sb.append("# CPU Information\n\n");
            sb.append("| Property | Value |\n");
            sb.append("|---|---|\n");
            sb.append("| Model | ").append(cpu.modelName()).append(" |\n");
            sb.append("| Physical Cores | ").append(cpu.physicalCores()).append(" |\n");
            sb.append("| Logical Cores | ").append(cpu.logicalCores()).append(" |\n");
            sb.append("| System Load | ").append(String.format("%.1f%%", cpu.systemLoadPercent())).append(" |\n");
            sb.append("| User Load | ").append(String.format("%.1f%%", cpu.userLoadPercent())).append(" |\n");
            sb.append("| Idle | ").append(String.format("%.1f%%", cpu.idlePercent())).append(" |\n");
            sb.append("| **Total Usage** | ").append(String.format("%.1f%%", cpu.systemLoadPercent() + cpu.userLoadPercent())).append(" |\n\n");

            if (cpu.perCoreLoadPercent().length > 0) {
                sb.append("## Per-Core Load\n\n");
                sb.append("| Core | Usage |\n");
                sb.append("|---|---|\n");
                for (int i = 0; i < cpu.perCoreLoadPercent().length; i++) {
                    sb.append("| Core ").append(i).append(" | ").append(String.format("%.1f%%", cpu.perCoreLoadPercent()[i])).append(" |\n");
                }
            }

            Map<String, Object> structured = Map.of(
                "modelName", cpu.modelName(),
                "physicalCores", cpu.physicalCores(),
                "logicalCores", cpu.logicalCores(),
                "systemLoadPercent", cpu.systemLoadPercent() + cpu.userLoadPercent(),
                "userLoadPercent", cpu.userLoadPercent(),
                "idlePercent", cpu.idlePercent(),
                "perCoreLoadPercent", cpu.perCoreLoadPercent()
            );

            return ToolResult.success(sb.toString(), structured);
        } catch (Exception e) {
            return ToolResult.error("Failed to get CPU information: " + e.getMessage());
        }
    }
}
