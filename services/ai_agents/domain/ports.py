"""Port interfaces (Protocols) for infrastructure adapters.

These protocols define the contracts that adapters must implement,
following the Dependency Inversion Principle.
"""

from typing import TYPE_CHECKING, Any, Dict, List, Optional, Protocol, runtime_checkable

from services.ai_agents.domain.values import (
    ChunkConfig,
    Document,
    PipelineDefinition,
    PipelineRun,
    PipelineStep,
)

if TYPE_CHECKING:
    from typing import Sequence


@runtime_checkable
class RAGPort(Protocol):
    """Protocol for RAG (Retrieval-Augmented Generation) operations."""

    def create_collection(self, name: str, dimension: int = 1536) -> Dict[str, Any]:
        """Create a new collection."""
        ...

    def index_document(
        self,
        collection: str,
        content: str,
        metadata: Optional[Dict[str, Any]] = None,
        chunk_config: Optional[ChunkConfig] = None,
    ) -> Dict[str, Any]:
        """Index a document into a collection."""
        ...

    def search(
        self,
        collection: str,
        query: str,
        top_k: int = 5,
        filters: Optional[Dict[str, Any]] = None,
    ) -> List[Any]:
        """Search for relevant documents."""
        ...

    def get_document(self, collection: str, document_id: str) -> Optional[Document]:
        """Get a document by ID."""
        ...

    def delete_document(self, collection: str, document_id: str) -> Dict[str, Any]:
        """Delete a document."""
        ...


@runtime_checkable
class PipelinePort(Protocol):
    """Protocol for pipeline operations."""

    def create_pipeline(
        self,
        name: str,
        steps: List[PipelineStep],
        description: Optional[str] = None,
        trigger: Optional[str] = None,
        timeout_seconds: int = 3600,
    ) -> Dict[str, Any]:
        """Create a new pipeline."""
        ...

    def get_pipeline(self, name: str) -> Optional[PipelineDefinition]:
        """Get a pipeline by name."""
        ...

    def list_pipelines(self) -> List[PipelineDefinition]:
        """List all pipelines."""
        ...

    def run_pipeline(
        self,
        pipeline_name: str,
        parameters: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        """Run a pipeline."""
        ...

    def get_run(self, run_id: str) -> Optional[PipelineRun]:
        """Get a pipeline run."""
        ...


@runtime_checkable
class LLMProvider(Protocol):
    """Protocol for LLM (Language Model) providers.

    This protocol abstracts the LLM interface, allowing the domain layer
    to work with any LLM implementation without depending on specific frameworks.
    """

    def invoke(self, messages: "Sequence[Any]") -> Any:
        """Invoke the LLM with a sequence of messages.

        Args:
            messages: Sequence of message objects (e.g., HumanMessage, SystemMessage).

        Returns:
            The LLM response (typically an AIMessage or similar).
        """
        ...

    def stream(self, messages: "Sequence[Any]") -> Any:
        """Stream responses from the LLM.

        Args:
            messages: Sequence of message objects.

        Yields:
            Chunks of the LLM response.
        """
        ...


@runtime_checkable
class ToolProvider(Protocol):
    """Protocol for tool providers.

    This protocol abstracts tool interfaces, allowing the domain layer
    to work with any tool implementation without depending on specific frameworks.
    """

    @property
    def name(self) -> str:
        """Tool name identifier."""
        ...

    @property
    def description(self) -> str:
        """Human-readable description of the tool's functionality."""
        ...

    def invoke(self, input: Any) -> Any:
        """Execute the tool with the given input.

        Args:
            input: Tool input data (format depends on tool implementation).

        Returns:
            Tool execution result.
        """
        ...


@runtime_checkable
class TextProcessorPort(Protocol):
    """Protocol for text processing operations.

    Defines the contract for text processing adapters that handle
    sentence splitting and heading detection.
    """

    def split_sentences(self, content: str) -> List[str]:
        """Split content into sentences.

        Args:
            content: Text content to split.

        Returns:
            List of sentence strings.
        """
        ...

    def match_headings(self, content: str) -> List[tuple[str, str]]:
        """Match markdown headings in content.

        Args:
            content: Text containing markdown headings.

        Returns:
            List of tuples (heading_level, heading_text).
        """
        ...


@runtime_checkable
class AgentOrchestrator(Protocol):
    """Protocol for agent orchestration.

    This defines the interface that any agent orchestrator (e.g., SupervisorAgent)
    must implement, allowing Application layer to depend on abstraction
    rather than concrete implementations.
    """

    @property
    def available_agents(self) -> List[str]:
        """Get list of available agent names."""
        ...

    @property
    def agent_descriptions(self) -> Dict[str, str]:
        """Get agent descriptions."""
        ...

    def invoke(self, input_data: Dict[str, Any]) -> Dict[str, Any]:
        """Invoke the orchestrator with input data."""
        ...
