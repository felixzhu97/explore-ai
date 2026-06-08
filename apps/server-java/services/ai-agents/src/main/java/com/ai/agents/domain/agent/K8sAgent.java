package com.ai.agents.domain.agent;

import com.ai.agents.domain.model.AgentRequest;
import com.ai.agents.domain.model.AgentResponse;
import com.ai.agents.domain.model.ToolResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * K8s (Kubernetes) operations agent.
 */
@Component
public class K8sAgent extends AbstractAiAgent {

    private static final String SYSTEM_PROMPT = """
            You are a Kubernetes operations assistant specialized in K8s cluster management.

            You can help with:
            - Deploying applications
            - Scaling workloads
            - Debugging pods and services
            - Managing namespaces
            - Viewing logs and events
            """;

    public K8sAgent() {
        super("k8s", "K8s Agent", "Kubernetes operations", AgentType.K8S);
    }

    @Override
    public boolean canHandle(AgentRequest request) {
        String msg = request.message().toLowerCase();
        return msg.contains("k8s") || msg.contains("kubernetes") ||
               msg.contains("pod") || msg.contains("deployment") ||
               msg.contains("cluster") || msg.contains("namespace");
    }

    @Override
    protected String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    protected String processWithContext(AgentRequest request, List<ToolResult> toolResults) {
        return """
                K8s Request received.

                Common operations:
                - kubectl get pods -n <namespace>
                - kubectl describe pod <pod-name>
                - kubectl logs <pod-name>
                - kubectl scale deployment <name> --replicas=<count>
                """;
    }
}
