"""Backward compatibility - imports moved to presentation/agents/protocols.py.

This module is kept for backward compatibility. New code should import from
services.ai_agents.presentation.agents.protocols instead.
"""

from services.ai_agents.presentation.agents.protocols import AgentProtocol, BaseAgent

__all__ = ["AgentProtocol", "BaseAgent"]
