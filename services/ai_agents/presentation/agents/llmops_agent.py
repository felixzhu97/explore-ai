"""LLMOps Agent implementation.

This agent specializes in machine learning model lifecycle, experiment tracking, and MLOps automation.
"""

from typing import Any, Dict, List, Optional

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool
from langgraph.graph import StateGraph, END

from services.ai_agents.infrastructure.base import BaseInfraAgent, AgentState
from services.ai_agents.presentation.agents.prompts import LLMOPS_SYSTEM_PROMPT


class LLMOpsAgent(BaseInfraAgent):
    """Agent for ML model lifecycle management and MLOps operations.
    
    This agent provides capabilities for:
    - Model registration and versioning
    - Experiment tracking and comparison
    - Training orchestration
    - Model evaluation and benchmarking
    - A/B testing and canary deployments
    - Performance monitoring and rollback
    
    Example:
        ```python
        from langchain_openai import ChatOpenAI
        from services.ai_agents.presentation.agents import LLMOpsAgent
        
        llm = ChatOpenAI(model="gpt-4")
        agent = LLMOpsAgent(llm=llm)
        
        # Register a model
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Register model 'sentiment-classifier' with framework 'pytorch'"
            )]
        })
        
        # Start training
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Train model 'sentiment-classifier' on dataset 'sentiment-train' for 10 epochs"
            )]
        })
        ```
    """
    
    def __init__(
        self,
        llm: BaseChatModel,
        tools: Optional[List[BaseTool]] = None,
        system_prompt: Optional[str] = None,
    ):
        """Initialize the LLMOps Agent.
        
        Args:
            llm: The language model for reasoning.
            tools: Optional list of tools (defaults to all LLMOps tools).
            system_prompt: Optional custom system prompt.
        """
        _tools = tools if tools is not None else []
        _prompt = system_prompt or LLMOPS_SYSTEM_PROMPT
        
        super().__init__(
            llm=llm,
            tools=_tools,
            system_prompt=_prompt,
            name="LLMOpsAgent",
            description="Manages ML model lifecycle, experiments, training, and deployments"
        )
    
    def create_graph(self) -> Runnable:
        """Create the LangGraph workflow for the LLMOps Agent.
        
        Returns:
            A compiled Runnable instance.
        """
        workflow = StateGraph(AgentState)
        
        workflow.add_node("supervisor", self._create_supervisor_node())
        
        workflow.set_entry_point("supervisor")
        workflow.add_edge("supervisor", END)
        
        return workflow.compile()
    
    def _create_supervisor_node(self):
        """Create the supervisor node that coordinates tool usage."""
        def supervisor_node(state: AgentState) -> Dict[str, Any]:
            """Supervisor node for routing and coordination."""
            messages = state.get("messages", [])
            if not messages:
                return {"messages": [], "context": {}}
            
            last_message = messages[-1]
            
            response = self.llm.bind_tools(
                self.tools,
                tool_choice="auto"
            ).invoke(
                [self._format_system_message()] + messages
            )
            
            return {
                "messages": [response],
                "context": {
                    "agent": self.name,
                    "task": str(last_message.content) if hasattr(last_message, 'content') else str(last_message)
                }
            }
        
        return supervisor_node
    
    def _format_system_message(self) -> BaseMessage:
        """Format the system message for the agent."""
        from langchain_core.messages import SystemMessage
        
        tool_descriptions = "\n".join(
            f"- {tool.name}: {tool.description}"
            for tool in self.tools
        )
        
        full_prompt = f"""{self.system_prompt}

Available Tools:
{tool_descriptions}

Instructions:
1. Analyze the model management request
2. Select appropriate tools for the task
3. Ensure proper logging and tracking
4. Monitor for successful completion
5. Provide clear feedback on operations
"""
        return SystemMessage(content=full_prompt)
    
    def register_model(
        self,
        name: str,
        description: Optional[str] = None,
        framework: str = "pytorch"
    ) -> Dict[str, Any]:
        """Convenience method for registering a model.
        
        Args:
            name: Model name.
            description: Model description.
            framework: ML framework.
            
        Returns:
            Registration result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Register model '{name}' with framework '{framework}'"
            )]
        })
        
        return result
    
    def train_model(
        self,
        model_name: str,
        dataset: str,
        epochs: int = 10,
        batch_size: int = 32,
        learning_rate: float = 0.001
    ) -> Dict[str, Any]:
        """Convenience method for training a model.
        
        Args:
            model_name: Model to train.
            dataset: Training dataset.
            epochs: Number of epochs.
            batch_size: Batch size.
            learning_rate: Learning rate.
            
        Returns:
            Training result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Train model '{model_name}' on dataset '{dataset}' "
                       f"for {epochs} epochs with batch size {batch_size}"
            )]
        })
        
        return result
    
    def evaluate_model(
        self,
        model_version: str,
        dataset: str,
        metrics: Optional[List[str]] = None
    ) -> Dict[str, Any]:
        """Convenience method for evaluating a model.
        
        Args:
            model_version: Model version to evaluate.
            dataset: Evaluation dataset.
            metrics: Metrics to compute.
            
        Returns:
            Evaluation result.
        """
        metrics_str = ", ".join(metrics) if metrics else "accuracy, precision, recall, f1"
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Evaluate model '{model_version}' on dataset '{dataset}' "
                       f"using metrics: {metrics_str}"
            )]
        })
        
        return result
    
    def deploy_model(
        self,
        model_name: str,
        version: str,
        replicas: int = 1,
        strategy: str = "rolling"
    ) -> Dict[str, Any]:
        """Convenience method for deploying a model.
        
        Args:
            model_name: Model name.
            version: Model version.
            replicas: Number of replicas.
            strategy: Deployment strategy.
            
        Returns:
            Deployment result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Deploy model '{model_name}' version '{version}' "
                       f"with {replicas} replicas using {strategy} strategy"
            )]
        })
        
        return result
    
    def configure_ab_test(
        self,
        model_a: str,
        model_b: str,
        traffic_split: Dict[str, float],
        success_metric: str = "accuracy"
    ) -> Dict[str, Any]:
        """Convenience method for configuring A/B test.
        
        Args:
            model_a: First model version.
            model_b: Second model version.
            traffic_split: Traffic percentages.
            success_metric: Success metric.
            
        Returns:
            A/B test configuration result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Configure A/B test between '{model_a}' and '{model_b}' "
                       f"with {traffic_split} traffic split"
            )]
        })
        
        return result
    
    def rollback_model(
        self,
        model_name: str,
        target_version: Optional[str] = None
    ) -> Dict[str, Any]:
        """Convenience method for rolling back a model.
        
        Args:
            model_name: Model name.
            target_version: Target version.
            
        Returns:
            Rollback result.
        """
        version_msg = f" to version '{target_version}'" if target_version else ""
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Rollback model '{model_name}'{version_msg}"
            )]
        })
        
        return result
    
    def list_models(self, stage: Optional[str] = None) -> Dict[str, Any]:
        """Convenience method for listing models.
        
        Args:
            stage: Optional stage filter.
            
        Returns:
            List of models.
        """
        stage_msg = f" in stage '{stage}'" if stage else ""
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"List all registered models{stage_msg}"
            )]
        })
        
        return result
