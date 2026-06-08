package com.ai.agents.domain.agent;

import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;
import com.ai.agents.domain.model.ToolResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Monitor agent for system monitoring and observability.
 */
@Component
public class MonitorAgent extends AbstractAiAgent {

    private static final String SYSTEM_PROMPT = """
            You are a System Monitoring assistant specialized in observability and monitoring.

            You can help with:
            - Checking system metrics
            - Analyzing logs
            - Setting up alerts
            - Creating dashboards
            - Investigating incidents
            """;

    public MonitorAgent() {
        super("monitor", "Monitor Agent", "System monitoring and observability", AgentType.MONITOR);
    }

    @Override
    public boolean canHandle(AgentRequest request) {
        String msg = request.message().toLowerCase();
        return msg.contains("monitor") || msg.contains("metric") ||
               msg.contains("log") || msg.contains("alert") ||
               msg.contains("prometheus") || msg.contains("grafana");
    }

    @Override
    protected String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String processWithContext(AgentRequest request, List<ToolResult> toolResults) {
        return """
                Monitor Request received.

                Available monitoring tools:
                - Prometheus metrics query
                - Grafana dashboard API
                - Log aggregation (Elasticsearch)
                - Alert management

                To access monitoring, use the monitoring service endpoints.
                """;
    }
}
