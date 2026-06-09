"""Agent protocols for dependency inversion.

This module defines interfaces (protocols) for agents, allowing the supervisor
to depend on abstractions rather than concrete implementations.
"""

from typing import TYPE_CHECKING, Any, Dict, List, Optional, Protocol, runtime_checkable
from abc import ABC, abstractmethod

if TYPE_CHECKING:
    from services.ai_agents.domain.ports import LLMProvider, ToolProvider


@runtime_checkable
class AgentProtocol(Protocol):
    """Protocol defining the interface for all agents.
    
    This allows the supervisor to work with any agent implementation
    without depending on concrete classes.
    """
    
    @property
    def name(self) -> str:
        """Agent name identifier."""
        ...
    
    @property
    def description(self) -> str:
        """Human-readable description of agent capabilities."""
        ...
    
    @property
    def tools(self) -> List["ToolProvider"]:
        """Tools available to this agent."""
        ...
    
    def invoke(self, input_data: Dict[str, Any]) -> Dict[str, Any]:
        """Invoke the agent with input data."""
        ...
    
    def batch(self, inputs: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Process a batch of inputs."""
        ...


class BaseAgent(ABC):
    """Abstract base class for all agents.
    
    Provides common functionality for all agent implementations.
    Subclasses should inherit from this class and implement the abstract methods.
    """
    
    def __init__(
        self,
        llm: "LLMProvider",
        name: str,
        description: str,
        tools: Optional[List["ToolProvider"]] = None,
    ):
        self._llm = llm
        self._name = name
        self._description = description
        self._tools = tools or []
    
    @property
    def name(self) -> str:
        return self._name
    
    @property
    def description(self) -> str:
        return self._description
    
    @property
    def tools(self) -> List["ToolProvider"]:
        return self._tools
    
    @abstractmethod
    def invoke(self, input_data: Dict[str, Any]) -> Dict[str, Any]:
        """Invoke the agent with input data.
        
        Args:
            input_data: Dictionary containing input data.
            
        Returns:
            Dictionary containing agent response.
        """
        pass
    
    def batch(self, inputs: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Process a batch of inputs.
        
        Default implementation processes sequentially.
        
        Args:
            inputs: List of input dictionaries.
            
        Returns:
            List of response dictionaries.
        """
        return [self.invoke(inp) for inp in inputs]
