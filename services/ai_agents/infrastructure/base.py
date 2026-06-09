"""Base classes for AI Infrastructure Agents.

This module provides the base agent class and state definitions
for all specialized agents in the AI infrastructure suite.
"""

from typing import Any, Dict, List, Optional, TypedDict

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
from langchain_core.runnables import Runnable
from langchain_core.tools import BaseTool
from langchain_core.utils.function_calling import convert_to_openai_function


class AgentState(TypedDict):
    """State schema for all agents."""
    messages: List[BaseMessage]
    context: Dict[str, Any]


class BaseInfraAgent:
    """Base class for all infrastructure agents.
    
    This class provides common functionality for:
    - LLM-based reasoning and tool usage
    - State management with LangGraph
    - Message handling and response formatting
    
    Subclasses should:
    1. Define specialized tools
    2. Implement create_graph() for workflow
    3. Set appropriate system prompts
    4. Override _format_system_message() if needed
    """
    
    def __init__(
        self,
        llm: BaseChatModel,
        tools: Optional[List[BaseTool]] = None,
        system_prompt: str = "",
        name: str = "BaseAgent",
        description: str = "Base infrastructure agent",
    ):
        """Initialize the base agent.
        
        Args:
            llm: Language model for reasoning.
            tools: Optional list of tools.
            system_prompt: System prompt for the agent.
            name: Agent name.
            description: Agent description.
        """
        self.llm = llm
        self.tools = tools or []
        self.system_prompt = system_prompt
        self.name = name
        self.description = description
        self._graph: Optional[Runnable] = None
    
    @property
    def available_tools(self) -> List[str]:
        """Get list of available tool names."""
        return [tool.name for tool in self.tools]
    
    def _format_system_message(self) -> BaseMessage:
        """Format the system message.
        
        Returns:
            System message for the agent.
        """
        from langchain_core.messages import SystemMessage
        return SystemMessage(content=self.system_prompt)
    
    def create_graph(self) -> Runnable:
        """Create the LangGraph workflow.
        
        Returns:
            Compiled runnable graph.
        """
        from langgraph.graph import StateGraph, END
        
        workflow = StateGraph(AgentState)
        workflow.add_node("agent", self._create_agent_node())
        workflow.set_entry_point("agent")
        workflow.add_edge("agent", END)
        
        return workflow.compile()
    
    def _create_agent_node(self):
        """Create the agent node that handles tool calling."""
        from langgraph.prebuilt import ToolNode
        
        # Create tool node for executing tools
        tool_node = ToolNode(self.tools) if self.tools else None
        
        def agent_node(state: AgentState) -> Dict[str, Any]:
            """Process messages and handle tool calls."""
            messages = state.get("messages", [])
            if not messages:
                return {"messages": [], "context": {}}
            
            # Get LLM with tools bound
            llm_with_tools = self.llm.bind_tools(
                self.tools,
                tool_choice="auto"
            )
            
            # Invoke LLM
            response = llm_with_tools.invoke(
                [self._format_system_message()] + messages
            )
            
            # Check if LLM wants to call tools
            if hasattr(response, 'tool_calls') and response.tool_calls:
                # Return response with tool calls - will be handled by ToolNode
                return {
                    "messages": [response],
                    "context": {"agent": self.name, "action": "tool_call"}
                }
            
            # No tool calls - extract clean content
            content = ""
            if hasattr(response, 'content'):
                content = response.content
            elif isinstance(response, str):
                content = response
            
            return {
                "messages": [AIMessage(content=content)],
                "context": {"agent": self.name, "action": "response"}
            }
        
        return agent_node
    
    def get_runnable(self) -> Runnable:
        """Get the compiled, executable runnable.
        
        Returns:
            Compiled Runnable.
        """
        if self._graph is None:
            self._graph = self.create_graph()
        return self._graph
    
    def invoke(self, input_data: Dict[str, Any]) -> Dict[str, Any]:
        """Invoke the agent with input data.
        
        Args:
            input_data: Dictionary with messages and context.
            
        Returns:
            Agent response.
        """
        runnable = self.get_runnable()
        return runnable.invoke(input_data)
    
    def stream(self, input_data: Dict[str, Any]):
        """Stream the agent response (synchronous).
        
        Args:
            input_data: Dictionary with messages and context.
            
        Yields:
            Streamed response chunks.
        """
        runnable = self.get_runnable()
        return runnable.stream(input_data)
    
    async def astream(self, input_data: Dict[str, Any]):
        """Async stream the agent response.
        
        Args:
            input_data: Dictionary with messages and context.
            
        Yields:
            Streamed response chunks.
        """
        runnable = self.get_runnable()
        if hasattr(runnable, 'astream'):
            async for chunk in runnable.astream(input_data):
                yield chunk
        else:
            # Fall back to sync stream
            for chunk in runnable.stream(input_data):
                yield chunk
