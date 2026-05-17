"""Repository interface for Pipeline operations."""
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Dict, Any, List

@dataclass(frozen=True)
class PipelineInfo:
    """Value object for pipeline metadata."""
    name: str
    status: str
    config: Dict[str, Any]

class PipelineRepository(ABC):
    """Abstract repository for Pipeline operations."""
    
    @abstractmethod
    def create_pipeline(
        self,
        name: str,
        config: Dict[str, Any]
    ) -> PipelineInfo:
        """Create a new pipeline."""
        pass
    
    @abstractmethod
    def get_pipeline(self, name: str) -> PipelineInfo:
        """Get pipeline details."""
        pass
    
    @abstractmethod
    def trigger_pipeline(
        self,
        name: str,
        params: Dict[str, Any]
    ) -> str:
        """Trigger a pipeline run. Returns run ID."""
        pass
    
    @abstractmethod
    def get_pipeline_status(self, name: str) -> str:
        """Get current pipeline status."""
        pass
    
    @abstractmethod
    def list_pipeline_runs(self, name: str) -> List[Dict[str, Any]]:
        """List all runs for a pipeline."""
        pass
