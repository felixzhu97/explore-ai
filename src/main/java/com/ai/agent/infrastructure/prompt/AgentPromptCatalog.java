package com.ai.agent.infrastructure.prompt;

import com.ai.agent.domain.model.AgentDefinition;
import com.ai.agent.domain.vo.AgentType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentPromptCatalog {

    public List<AgentDefinition> defaultAgents() {
        return List.of(
                AgentDefinition.create(
                        AgentType.supervisor(),
                        "Supervisor",
                        "Multi-agent orchestrator - coordinates specialized agents for complex tasks",
                        """
                                You are the Supervisor orchestrator. Analyze the user request and decide
                                which specialized worker agent should handle it. Prefer the smallest set of workers.
                                Available workers include: k8s, monitoring, aiops, vectordb.
                                """),
                AgentDefinition.create(
                        AgentType.of("k8s"),
                        "K8s Agent",
                        "Kubernetes cluster operations advisor",
                        """
                                You are a Kubernetes expert. Help with pods, deployments, services, scaling,
                                and cluster troubleshooting. Be concise and actionable. If you lack live cluster
                                access, explain the kubectl/commands the operator should run.
                                """),
                AgentDefinition.create(
                        AgentType.of("monitoring"),
                        "Monitoring Agent",
                        "Metrics, alerts, and observability advisor",
                        """
                                You are a monitoring and observability expert. Help with metrics queries,
                                alert design, SLOs, and dashboards. Prefer Prometheus/Grafana-style guidance.
                                """),
                AgentDefinition.create(
                        AgentType.of("aiops"),
                        "AIOps Agent",
                        "Incident response and anomaly detection advisor",
                        """
                                You are an AIOps specialist. Help detect anomalies, triage incidents,
                                suggest root-cause hypotheses, and recommend remediation steps.
                                """),
                AgentDefinition.create(
                        AgentType.of("vectordb"),
                        "VectorDB / RAG Agent",
                        "Document retrieval and knowledge-base Q&A advisor",
                        """
                                You are a RAG and vector database expert. Help with document indexing,
                                semantic search, chunking strategies, and grounding answers in retrieved context.
                                Use document search tools when available.
                                """));
    }
}
