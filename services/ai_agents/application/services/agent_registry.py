"""Agent Registry for dynamic agent creation and management.

This module provides a centralized registry for agent classes,
enabling dependency injection and dynamic agent instantiation.
"""

from typing import Dict, Type, TYPE_CHECKING

if TYPE_CHECKING:
    from services.ai_agents.presentation.agents.protocols import AgentProtocol


class AgentRegistry:
    """Registry for agent classes enabling dynamic instantiation.
    
    This class maintains a mapping of agent names to their classes,
    allowing the Supervisor to create agents on demand without
    direct dependencies on concrete implementations.
    """
    
    _agents: Dict[str, Type["AgentProtocol"]] = {}
    
    @classmethod
    def register(cls, name: str, agent_class: Type["AgentProtocol"]) -> None:
        """Register an agent class.
        
        Args:
            name: Agent identifier (e.g., "monitoring", "model", "rag").
            agent_class: The agent class to register.
            
        Raises:
            ValueError: If agent name is already registered.
        """
        if name in cls._agents:
            raise ValueError(f"Agent '{name}' is already registered. Use replace() to override.")
        cls._agents[name] = agent_class
    
    @classmethod
    def replace(cls, name: str, agent_class: Type["AgentProtocol"]) -> None:
        """Replace an existing agent class.
        
        Args:
            name: Agent identifier.
            agent_class: The new agent class to register.
        """
        cls._agents[name] = agent_class
    
    @classmethod
    def create(cls, name: str, *args, **kwargs) -> "AgentProtocol":
        """Create an agent instance by name.
        
        Args:
            name: Agent identifier.
            *args: Positional arguments to pass to the agent constructor.
            **kwargs: Keyword arguments to pass to the agent constructor.
            
        Returns:
            An instance of the registered agent.
            
        Raises:
            ValueError: If agent name is not registered.
        """
        if name not in cls._agents:
            available = ", ".join(cls._agents.keys()) or "none"
            raise ValueError(
                f"Unknown agent: '{name}'. Available agents: {available}"
            )
        return cls._agents[name](*args, **kwargs)
    
    @classmethod
    def get(cls, name: str) -> Type["AgentProtocol"]:
        """Get an agent class by name.
        
        Args:
            name: Agent identifier.
            
        Returns:
            The registered agent class.
            
        Raises:
            ValueError: If agent name is not registered.
        """
        if name not in cls._agents:
            available = ", ".join(cls._agents.keys()) or "none"
            raise ValueError(
                f"Unknown agent: '{name}'. Available agents: {available}"
            )
        return cls._agents[name]
    
    @classmethod
    def is_registered(cls, name: str) -> bool:
        """Check if an agent is registered.
        
        Args:
            name: Agent identifier.
            
        Returns:
            True if agent is registered, False otherwise.
        """
        return name in cls._agents
    
    @classmethod
    def list_agents(cls) -> list[str]:
        """List all registered agent names.
        
        Returns:
            List of registered agent identifiers.
        """
        return list(cls._agents.keys())
    
    @classmethod
    def clear(cls) -> None:
        """Clear all registered agents. Primarily for testing."""
        cls._agents.clear()
