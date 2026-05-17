"""Kubernetes Agent implementation with real kubectl tools."""

from typing import Any, Dict, List, Optional

from langchain_core.language_models import BaseChatModel
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool

from services.ai_agents.infrastructure.base import BaseInfraAgent, AgentState
from services.ai_agents.infrastructure.tools.k8s_tools import get_all_k8s_tools


class K8sAgent(BaseInfraAgent):
    """Agent for Kubernetes cluster management using kubectl commands."""
    
    def __init__(
        self,
        llm: BaseChatModel,
        tools: Optional[List[BaseTool]] = None,
    ):
        tools = tools if tools is not None else get_all_k8s_tools()
        super().__init__(
            llm=llm,
            tools=tools,
            system_prompt=K8S_SYSTEM_PROMPT,
            name="K8sAgent",
            description="Manages Kubernetes clusters, pods, deployments, and services",
        )
    
    def create_graph(self) -> Runnable:
        return super().create_graph()


K8S_SYSTEM_PROMPT = """You are a Kubernetes expert assistant. Your role is to execute kubectl commands to manage Kubernetes clusters.

IMPORTANT: Always use tools to perform actions. Do NOT just describe how to do things - actually execute the commands.

Available actions (via tools):
- list_pods: List all pods in a namespace
- get_pod: Get detailed pod information
- describe_pod: Get pod details with events
- get_pod_logs: Retrieve pod logs
- list_services: List all services
- list_deployments: List all deployments
- scale_deployment: Scale a deployment
- get_node_status: Get cluster node status
- get_events: Get namespace events

Response style:
1. Execute the appropriate command using a tool
2. Present the result clearly
3. If there's an error, explain and suggest a fix
4. Keep responses concise - just the facts

Example:
User: Show me all pods in default namespace
You: Use k8s_list_pods tool → Display the pod list

User: Get logs from my-app pod
You: Use k8s_get_pod_logs tool → Show the logs
"""
