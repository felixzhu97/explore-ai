"""Supervisor Agent implementation for multi-agent coordination.

This agent orchestrates multiple specialized agents to handle complex
tasks that span multiple domains of AI infrastructure.
"""

from typing import Any, Dict, List, Literal, Optional, Sequence, TypedDict

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import (
    BaseMessage,
    HumanMessage,
    AIMessage,
    SystemMessage,
    ToolMessage,
)
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool
from langgraph.graph import StateGraph, END

from services.ai_agents.core.base import BaseInfraAgent
from services.ai_agents.core.prompts import SUPERVISOR_SYSTEM_PROMPT
from services.ai_agents.agents.vector_db_agent import VectorDBAgent
from services.ai_agents.agents.k8s_agent import K8sAgent
from services.ai_agents.agents.monitoring_agent import MonitoringAgent
from services.ai_agents.agents.model_agent import ModelAgent
from services.ai_agents.agents.rag_agent import RAGAgent
from services.ai_agents.agents.llmops_agent import LLMOpsAgent
from services.ai_agents.agents.feature_store_agent import FeatureStoreAgent
from services.ai_agents.agents.pipeline_agent import PipelineAgent
from services.ai_agents.agents.aiops_agent import AIOpsAgent
from services.ai_agents.agents.video_agent import VideoAgent


class SupervisorState(TypedDict):
    """State schema for the supervisor agent."""
    messages: List[BaseMessage]
    current_agent: Optional[str]
    agent_results: Dict[str, Any]
    task: Optional[str]
    pending_actions: List[str]


class SupervisorAgent:
    """Supervisor agent that coordinates multiple specialized agents.
    
    This agent acts as a coordinator that:
    - Intelligently routes tasks to appropriate specialized agents
    - Manages multi-agent workflows
    - Aggregates results from different agents
    - Handles cross-cutting concerns
    
    Supported Agents:
        - VectorDBAgent: Vector database operations
        - K8sAgent: Kubernetes cluster management
        - MonitoringAgent: Observability and monitoring
        - ModelAgent: ML model lifecycle management
    
    Example:
        ```python
        from langchain_openai import ChatOpenAI
        from services.ai_agents.agents import SupervisorAgent
        
        llm = ChatOpenAI(model="gpt-4")
        supervisor = SupervisorAgent(llm=llm)
        
        # Coordinate multiple agents
        result = supervisor.invoke({
            "messages": [HumanMessage(
                content="Deploy the new model and verify it's running properly"
            )]
        })
        ```
    """
    
    AGENT_ROUTING = {
        "vector": ["vector", "embedding", "search", "collection", "chroma", "pinecone"],
        "kubernetes": ["k8s", "kubernetes", "pod", "deployment", "service", "cluster", "scale", "namespace"],
        "monitoring": ["monitor", "metric", "log", "alert", "prometheus", "grafana", "elasticsearch", "observe"],
        "model": ["model", "deploy", "ml", "training", "version", "rollback", "a/b", "canary"],
        "rag": ["rag", "document", "knowledge", "retrieval", "index", "chunk", "query", "search docs"],
        "llmops": ["llmops", "experiment", "train", "evaluate", "benchmark", "metrics"],
        "feature_store": ["feature", "features", "feature store", "transformation", "materialize"],
        "pipeline": ["pipeline", "workflow", "orchestrate", "step", "dag", "airflow"],
        "aiops": ["aiops", "anomaly", "incident", "root cause", "investigate", "alert"],
        "video": ["video", "generate video", "text-to-video", "text to video", "t2v", "animation", "clip"],
    }
    
    def __init__(
        self,
        llm: BaseChatModel,
        vector_agent: Optional[VectorDBAgent] = None,
        k8s_agent: Optional[K8sAgent] = None,
        monitoring_agent: Optional[MonitoringAgent] = None,
        model_agent: Optional[ModelAgent] = None,
        rag_agent: Optional[RAGAgent] = None,
        llmops_agent: Optional[LLMOpsAgent] = None,
        feature_store_agent: Optional[FeatureStoreAgent] = None,
        pipeline_agent: Optional[PipelineAgent] = None,
        aiops_agent: Optional[AIOpsAgent] = None,
        video_agent: Optional[VideoAgent] = None,
        system_prompt: Optional[str] = None,
    ):
        """Initialize the Supervisor Agent.
        
        Args:
            llm: The language model for reasoning and routing.
            vector_agent: Optional VectorDB agent instance.
            k8s_agent: Optional K8s agent instance.
            monitoring_agent: Optional Monitoring agent instance.
            model_agent: Optional Model agent instance.
            rag_agent: Optional RAG agent instance.
            llmops_agent: Optional LLMOps agent instance.
            feature_store_agent: Optional Feature Store agent instance.
            pipeline_agent: Optional Pipeline agent instance.
            aiops_agent: Optional AIOps agent instance.
            video_agent: Optional Video agent instance.
            system_prompt: Optional custom system prompt.
        """
        self.llm = llm
        self.system_prompt = system_prompt or SUPERVISOR_SYSTEM_PROMPT
        
        self.agents: Dict[str, BaseInfraAgent] = {}
        
        if vector_agent:
            self.agents["vector"] = vector_agent
        if k8s_agent:
            self.agents["kubernetes"] = k8s_agent
        if monitoring_agent:
            self.agents["monitoring"] = monitoring_agent
        if model_agent:
            self.agents["model"] = model_agent
        if rag_agent:
            self.agents["rag"] = rag_agent
        if llmops_agent:
            self.agents["llmops"] = llmops_agent
        if feature_store_agent:
            self.agents["feature_store"] = feature_store_agent
        if pipeline_agent:
            self.agents["pipeline"] = pipeline_agent
        if aiops_agent:
            self.agents["aiops"] = aiops_agent
        if video_agent:
            self.agents["video"] = video_agent
        
        self._graph: Optional[Runnable] = None
    
    def add_agent(self, name: str, agent: BaseInfraAgent) -> None:
        """Add a specialized agent to the supervisor.
        
        Args:
            name: Agent identifier (vector, kubernetes, monitoring, model).
            agent: Agent instance.
        """
        self.agents[name] = agent
    
    def remove_agent(self, name: str) -> None:
        """Remove a specialized agent from the supervisor.
        
        Args:
            name: Agent identifier.
        """
        if name in self.agents:
            del self.agents[name]
    
    def _route_to_agent(self, task: str) -> List[str]:
        """Determine which agents should handle the task.
        
        Args:
            task: The task description.
            
        Returns:
            List of agent names to route to.
        """
        task_lower = task.lower()
        
        # Special case: "list all available agents" queries - return empty list
        # to indicate no agent routing needed, just return agent list
        meta_keywords = [
            '列出所有', '列出可用', 'list all', 'list available', 'available agents',
            '所有 agent', '所有智能体', '有哪些 agent', 'show agents', 'what agents',
            '有哪些', '有什么 agent', '可用 agent'
        ]
        if any(kw in task_lower for kw in meta_keywords):
            return []  # Empty list signals to return agent info directly
        
        matched_agents = []
        
        for agent_name, keywords in self.AGENT_ROUTING.items():
            if agent_name in self.agents:
                for keyword in keywords:
                    if keyword in task_lower:
                        if agent_name not in matched_agents:
                            matched_agents.append(agent_name)
                        break
        
        if not matched_agents:
            matched_agents = list(self.agents.keys())
        
        return matched_agents
    
    def _create_agent_node(self, agent_name: str):
        """Create a node that executes a specific agent.
        
        Args:
            agent_name: Name of the agent.
            
        Returns:
            Node function.
        """
        agent = self.agents.get(agent_name)
        
        if agent is None:
            def fallback_node(state: SupervisorState) -> Dict[str, Any]:
                return {
                    "messages": state["messages"] + [
                        AIMessage(content=f"Agent '{agent_name}' is not available.")
                    ],
                    "agent_results": {**state.get("agent_results", {}), agent_name: {"error": "Agent not found"}}
                }
            return fallback_node
        
        def agent_node(state: SupervisorState) -> Dict[str, Any]:
            """Execute the agent with the current task."""
            task = state.get("task", "")
            messages = state.get("messages", [])
            
            if not messages:
                return {"messages": [], "agent_results": {}}
            
            try:
                result = agent.invoke({
                    "messages": messages
                })
                
                agent_messages = result.get("messages", [])
                agent_result = result.get("result", result)
                
                return {
                    "messages": state["messages"] + agent_messages,
                    "agent_results": {
                        **state.get("agent_results", {}),
                        agent_name: agent_result
                    }
                }
            except Exception as e:
                return {
                    "messages": state["messages"],
                    "agent_results": {
                        **state.get("agent_results", {}),
                        agent_name: {"error": str(e)}
                    }
                }
        
        return agent_node
    
    def create_graph(self) -> StateGraph:
        """Create the LangGraph workflow for the Supervisor.
        
        Returns:
            A configured StateGraph instance.
        """
        workflow = StateGraph(SupervisorState)
        
        workflow.add_node("router", self._create_router_node())
        
        for agent_name in self.agents.keys():
            workflow.add_node(agent_name, self._create_agent_node(agent_name))
        
        def should_continue(state: SupervisorState) -> Literal["router", "finish"]:
            """Determine if more agents need to be invoked or if we're done."""
            pending = state.get("pending_actions", [])
            if pending:
                return "router"
            return "finish"
        
        workflow.add_conditional_edges(
            "router",
            self._check_pending_actions,
            {
                agent_name: agent_name for agent_name in self.agents.keys()
            }  # No mapping needed for END - function returns END directly
        )
        
        for agent_name in self.agents.keys():
            workflow.add_edge(agent_name, END)
        
        workflow.set_entry_point("router")
        
        return workflow
    
    def _check_pending_actions(self, state: SupervisorState) -> Optional[str]:
        """Check if there are pending actions."""
        pending = state.get("pending_actions", [])
        if pending:
            return pending[0]
        return END  # Return END when no pending actions
    
    def _create_router_node(self):
        """Create the router node that coordinates agent execution."""
        def router_node(state: SupervisorState) -> Dict[str, Any]:
            """Route the task to appropriate agents."""
            messages = state.get("messages", [])
            
            if not messages:
                return {
                    "current_agent": None,
                    "pending_actions": [],
                    "task": None
                }
            
            last_message = messages[-1]
            task = str(last_message.content) if hasattr(last_message, 'content') else str(last_message)
            
            target_agents = self._route_to_agent(task)
            
            # Special case: meta query (empty target_agents) - return agent list directly
            if not target_agents:
                agent_list = ", ".join([
                    f"{name}: {agent.description}" 
                    for name, agent in self.agents.items()
                ])
                return {
                    "current_agent": None,
                    "pending_actions": [],
                    "task": None,
                    "messages": state["messages"] + [
                        AIMessage(content=f"以下是所有可用的 Agent:\n\n{agent_list}")
                    ],
                    "agent_results": {}
                }
            
            return {
                "current_agent": target_agents[0] if target_agents else None,
                "pending_actions": target_agents,
                "task": task,
                "messages": state["messages"] + [
                    AIMessage(content=f"Routing to: {', '.join(target_agents)}")
                ]
            }
        
        return router_node
    
    def get_runnable(self) -> Runnable:
        """Get the compiled, executable runnable.
        
        Returns:
            Compiled Runnable.
        """
        if self._graph is None:
            self._graph = self.create_graph().compile()
        return self._graph
    
    def invoke(self, input_data: Dict[str, Any]) -> Dict[str, Any]:
        """Invoke the supervisor with input data.
        
        Args:
            input_data: Dictionary with messages and context.
            
        Returns:
            Aggregated results from all agents.
        """
        runnable = self.get_runnable()
        return runnable.invoke(input_data)
    
    def invoke_single_agent(
        self,
        agent_name: str,
        task: str
    ) -> Dict[str, Any]:
        """Invoke a specific agent directly.
        
        Args:
            agent_name: Name of the agent to invoke.
            task: Task description.
            
        Returns:
            Agent result.
        """
        agent = self.agents.get(agent_name)
        if agent is None:
            return {"error": f"Agent '{agent_name}' not found"}
        
        return agent.invoke({
            "messages": [HumanMessage(content=task)]
        })
    
    @property
    def available_agents(self) -> List[str]:
        """Get list of available agent names."""
        return list(self.agents.keys())
    
    @property
    def agent_descriptions(self) -> Dict[str, str]:
        """Get descriptions of available agents."""
        return {
            name: agent.description for name, agent in self.agents.items()
        }


def create_supervisor_with_defaults(
    llm: BaseChatModel,
    **agent_kwargs
) -> SupervisorAgent:
    """Create a supervisor with all default agents.
    
    Args:
        llm: Language model for all agents.
        **agent_kwargs: Additional kwargs for individual agents.
    
    Returns:
        Configured SupervisorAgent with all default agents.
    """
    vector_agent = VectorDBAgent(llm=llm, **agent_kwargs.get("vector", {}))
    k8s_agent = K8sAgent(llm=llm, **agent_kwargs.get("k8s", {}))
    monitoring_agent = MonitoringAgent(llm=llm, **agent_kwargs.get("monitoring", {}))
    model_agent = ModelAgent(llm=llm, **agent_kwargs.get("model", {}))
    
    return SupervisorAgent(
        llm=llm,
        vector_agent=vector_agent,
        k8s_agent=k8s_agent,
        monitoring_agent=monitoring_agent,
        model_agent=model_agent,
    )
