"""Pipeline Agent implementation.

This agent specializes in orchestrating ML/DevOps pipelines and workflow automation.
"""

from typing import Any, Dict, List, Optional

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool
from langgraph.graph import StateGraph, END

from services.ai_agents.infrastructure.base import BaseInfraAgent, AgentState
from services.ai_agents.presentation.agents.prompts import PIPELINE_SYSTEM_PROMPT


class PipelineAgent(BaseInfraAgent):
    """Agent for pipeline orchestration and workflow automation.
    
    This agent provides capabilities for:
    - Pipeline definition and configuration
    - Multi-step workflow orchestration
    - Pipeline execution and monitoring
    - Retry and error handling
    - Integration with external systems
    - Execution history and analytics
    
    Example:
        ```python
        from langchain_openai import ChatOpenAI
        from services.ai_agents.presentation.agents import PipelineAgent
        
        llm = ChatOpenAI(model="gpt-4")
        agent = PipelineAgent(llm=llm)
        
        # Create a pipeline
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Create pipeline 'etl-pipeline' with steps: "
                       "extract_data, transform_data, load_data"
            )]
        })
        
        # Run a pipeline
        result = agent.invoke({
            "messages": [HumanMessage(
                content="Run pipeline 'etl-pipeline' with parameters: date=today"
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
        """Initialize the Pipeline Agent.
        
        Args:
            llm: The language model for reasoning.
            tools: Optional list of tools (defaults to all pipeline tools).
            system_prompt: Optional custom system prompt.
        """
        _tools = tools if tools is not None else []
        _prompt = system_prompt or PIPELINE_SYSTEM_PROMPT
        
        super().__init__(
            llm=llm,
            tools=_tools,
            system_prompt=_prompt,
            name="PipelineAgent",
            description="Orchestrates ML/DevOps pipelines and workflow automation"
        )
    
    def create_graph(self) -> Runnable:
        """Create the LangGraph workflow for the Pipeline Agent.
        
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
1. Understand the workflow requirements
2. Define clear step dependencies
3. Ensure idempotency for safe retries
4. Monitor execution progress
5. Handle errors gracefully
"""
        return SystemMessage(content=full_prompt)
    
    def create_pipeline(
        self,
        name: str,
        steps: List[Dict[str, Any]],
        description: Optional[str] = None,
        trigger: Optional[str] = None
    ) -> Dict[str, Any]:
        """Convenience method for creating a pipeline.
        
        Args:
            name: Pipeline name.
            steps: Pipeline steps.
            description: Optional description.
            trigger: Trigger configuration.
            
        Returns:
            Creation result.
        """
        step_names = ", ".join(s["name"] for s in steps)
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Create pipeline '{name}' with steps: {step_names}"
            )]
        })
        
        return result
    
    def run_pipeline(
        self,
        pipeline_name: str,
        parameters: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Convenience method for running a pipeline.
        
        Args:
            pipeline_name: Pipeline name.
            parameters: Run parameters.
            
        Returns:
            Run result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Run pipeline '{pipeline_name}'"
            )]
        })
        
        return result
    
    def get_pipeline(self, pipeline_name: str) -> Dict[str, Any]:
        """Convenience method for getting pipeline details.
        
        Args:
            pipeline_name: Pipeline name.
            
        Returns:
            Pipeline details.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Get details for pipeline '{pipeline_name}'"
            )]
        })
        
        return result
    
    def list_pipelines(self) -> Dict[str, Any]:
        """Convenience method for listing pipelines.
        
        Returns:
            List of pipelines.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content="List all available pipelines"
            )]
        })
        
        return result
    
    def get_run_status(self, run_id: str) -> Dict[str, Any]:
        """Convenience method for getting run status.
        
        Args:
            run_id: Run ID.
            
        Returns:
            Run status.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Get status for run '{run_id}'"
            )]
        })
        
        return result
    
    def list_runs(
        self,
        pipeline_name: Optional[str] = None,
        status: Optional[str] = None
    ) -> Dict[str, Any]:
        """Convenience method for listing runs.
        
        Args:
            pipeline_name: Optional pipeline filter.
            status: Optional status filter.
            
        Returns:
            List of runs.
        """
        filters = []
        if pipeline_name:
            filters.append(f"pipeline '{pipeline_name}'")
        if status:
            filters.append(f"status '{status}'")
        
        filter_str = f" with {', '.join(filters)}" if filters else ""
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"List pipeline runs{filter_str}"
            )]
        })
        
        return result
    
    def cancel_run(self, run_id: str) -> Dict[str, Any]:
        """Convenience method for canceling a run.
        
        Args:
            run_id: Run ID.
            
        Returns:
            Cancel result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Cancel pipeline run '{run_id}'"
            )]
        })
        
        return result
    
    def add_step(
        self,
        pipeline_name: str,
        step_name: str,
        step_type: str,
        depends_on: Optional[List[str]] = None,
        config: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Convenience method for adding a step.
        
        Args:
            pipeline_name: Pipeline name.
            step_name: Step name.
            step_type: Step type.
            depends_on: Dependencies.
            config: Step configuration.
            
        Returns:
            Add result.
        """
        deps_str = f" depending on {', '.join(depends_on)}" if depends_on else ""
        
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Add step '{step_name}' of type '{step_type}' to pipeline '{pipeline_name}'{deps_str}"
            )]
        })
        
        return result
    
    def delete_pipeline(self, pipeline_name: str) -> Dict[str, Any]:
        """Convenience method for deleting a pipeline.
        
        Args:
            pipeline_name: Pipeline name.
            
        Returns:
            Delete result.
        """
        result = self.invoke({
            "messages": [HumanMessage(
                content=f"Delete pipeline '{pipeline_name}'"
            )]
        })
        
        return result
