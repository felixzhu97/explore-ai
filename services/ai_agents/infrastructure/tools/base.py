"""Base classes and utilities for tools.

This module provides the foundation for standardized tool creation patterns
across all tool files in the ai_agents service.
"""

from abc import ABC, abstractmethod
from typing import Any, Dict, List, Callable, Optional
from dataclasses import dataclass
import logging

logger = logging.getLogger(__name__)


@dataclass
class ToolResult:
    """Structured result from tool execution.

    Provides a consistent return type for all tool implementations,
    enabling better error handling and result processing.
    """
    success: bool
    data: Any
    error: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        """Convert result to dictionary for serialization."""
        return {
            "success": self.success,
            "data": self.data,
            "error": self.error
        }

    @classmethod
    def ok(cls, data: Any) -> "ToolResult":
        """Create a successful result."""
        return cls(success=True, data=data, error=None)

    @classmethod
    def fail(cls, error: str, data: Any = None) -> "ToolResult":
        """Create a failed result."""
        return cls(success=False, data=data, error=error)


class BaseTool(ABC):
    """Abstract base class for all tools.

    Provides a consistent interface for tool execution and ensures
    all tools implement the required execute method.
    """

    def __init__(self, name: str, description: str):
        """Initialize the tool.

        Args:
            name: Unique identifier for the tool
            description: Human-readable description of the tool's purpose
        """
        self.name = name
        self.description = description

    @abstractmethod
    def execute(self, **kwargs) -> ToolResult:
        """Execute the tool with given arguments.

        Args:
            **kwargs: Tool-specific arguments

        Returns:
            ToolResult: Structured result containing success status,
                       data, and optional error message
        """
        pass

    def __call__(self, **kwargs) -> str:
        """Make the tool callable for LangChain compatibility.

        Converts ToolResult to string representation while maintaining
        backward compatibility with existing code expecting string returns.

        Args:
            **kwargs: Tool-specific arguments

        Returns:
            str: String representation of the result
        """
        result = self.execute(**kwargs)
        if not result.success:
            logger.warning(f"Tool {self.name} failed: {result.error}")
            return f"Error: {result.error}"
        return str(result.data)

    def __repr__(self) -> str:
        """String representation for debugging."""
        return f"{self.__class__.__name__}(name={self.name!r})"


def create_tool(
    name: str,
    description: str,
    func: Callable[..., ToolResult]
) -> BaseTool:
    """Factory function to create a tool from an implementation function.

    Wraps a function that returns ToolResult into a BaseTool subclass,
    enabling consistent tool creation patterns.

    Args:
        name: Unique identifier for the tool
        description: Human-readable description of the tool's purpose
        func: Implementation function that takes tool arguments and
              returns a ToolResult

    Returns:
        BaseTool: An instance of a concrete tool class

    Example:
        def add_impl(a: int, b: int) -> ToolResult:
            return ToolResult.ok(a + b)

        add_tool = create_tool(
            name="add",
            description="Add two numbers",
            func=add_impl
        )
    """

    class ConcreteTool(BaseTool):
        """Concrete tool implementation wrapping the provided function."""

        def __init__(self):
            super().__init__(name, description)
            self._func = func

        def execute(self, **kwargs) -> ToolResult:
            """Execute the wrapped function with error handling."""
            try:
                logger.debug(f"Executing tool {name} with args: {kwargs}")
                return self._func(**kwargs)
            except TypeError as e:
                error_msg = f"Invalid arguments for tool {name}: {e}"
                logger.error(error_msg)
                return ToolResult.fail(error_msg)
            except Exception as e:
                error_msg = f"Tool {name} error: {e}"
                logger.exception(error_msg)
                return ToolResult.fail(str(e))

    return ConcreteTool()


def create_langchain_tool(
    name: str,
    description: str,
    func: Callable[..., ToolResult],
    args_schema: Optional[type] = None
):
    """Create a tool compatible with LangChain's @tool decorator pattern.

    This factory creates a tool that can be used both as a standalone
    BaseTool and integrated with LangChain's tool system.

    Args:
        name: Unique identifier for the tool
        description: Human-readable description of the tool's purpose
        func: Implementation function that returns ToolResult
        args_schema: Optional Pydantic model for input validation

    Returns:
        A callable tool instance that returns strings (for LangChain compat)
        but also has an execute method returning ToolResult
    """
    tool_instance = create_tool(name, description, func)

    # Create a wrapper that mimics LangChain's @tool decorator behavior
    def wrapper(**kwargs) -> str:
        return tool_instance(**kwargs)

    # Attach metadata for LangChain compatibility
    wrapper.name = name
    wrapper.description = description
    wrapper.args_schema = args_schema
    wrapper.execute = tool_instance.execute
    wrapper.tool_class = tool_instance.__class__.__name__

    return wrapper
