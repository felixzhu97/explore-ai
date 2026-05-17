"""LangChain adapter implementations for domain protocols.

This module provides concrete implementations of the domain protocols
by wrapping LangChain components, following the Dependency Inversion Principle.
"""

from typing import Any, Sequence

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import BaseMessage
from langchain_core.outputs import ChatResult
from langchain_core.runnables import Runnable

from services.ai_agents.domain.ports import LLMProvider, ToolProvider


class LangChainLLMAdapter(LLMProvider):
    """Adapter wrapping LangChain's BaseChatModel to implement LLMProvider.

    This adapter allows LangChain LLM implementations to be used
    wherever an LLMProvider is expected.
    """

    def __init__(self, llm: BaseChatModel) -> None:
        """Initialize the adapter with a LangChain LLM.

        Args:
            llm: A LangChain BaseChatModel instance.
        """
        self._llm = llm

    def invoke(self, messages: Sequence[Any]) -> Any:
        """Invoke the LLM with a sequence of messages.

        Args:
            messages: Sequence of message objects (e.g., HumanMessage, SystemMessage).

        Returns:
            The LLM response (typically an AIMessage or similar).
        """
        return self._llm.invoke(messages)

    def stream(self, messages: Sequence[Any]) -> Any:
        """Stream responses from the LLM.

        Args:
            messages: Sequence of message objects.

        Yields:
            Chunks of the LLM response.
        """
        return self._llm.stream(messages)


class LangChainToolAdapter(ToolProvider):
    """Adapter wrapping LangChain's BaseTool to implement ToolProvider.

    This adapter allows LangChain tool implementations to be used
    wherever a ToolProvider is expected.
    """

    def __init__(self, tool: Any) -> None:
        """Initialize the adapter with a LangChain tool.

        Args:
            tool: A LangChain BaseTool instance or similar callable tool.
        """
        self._tool = tool

    @property
    def name(self) -> str:
        """Tool name identifier."""
        return self._tool.name if hasattr(self._tool, "name") else getattr(self._tool, "__name__", str(self._tool))

    @property
    def description(self) -> str:
        """Human-readable description of the tool's functionality."""
        return self._tool.description if hasattr(self._tool, "description") else ""

    def invoke(self, input: Any) -> Any:
        """Execute the tool with the given input.

        Args:
            input: Tool input data.

        Returns:
            Tool execution result.
        """
        return self._tool.invoke(input)


__all__ = [
    "LangChainLLMAdapter",
    "LangChainToolAdapter",
]
