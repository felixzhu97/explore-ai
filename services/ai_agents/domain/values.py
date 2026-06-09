"""Domain value objects for AI Infrastructure Agents.

This module defines pure domain types without any infrastructure dependencies.
All types use frozen dataclass for immutability.
"""

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional


class DistanceMetric(str, Enum):
    COSINE = "cosine"
    EUCLIDEAN = "euclidean"
    DOT_PRODUCT = "dotproduct"


class PipelineStatus(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    SUCCEEDED = "succeeded"
    FAILED = "failed"
    CANCELLED = "cancelled"


class ModelStage(str, Enum):
    REGISTERED = "registered"
    STAGING = "staging"
    PRODUCTION = "production"
    ARCHIVED = "archived"


class AlertSeverity(str, Enum):
    CRITICAL = "critical"
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"
    INFO = "info"


class IncidentStatus(str, Enum):
    OPEN = "open"
    INVESTIGATING = "investigating"
    IDENTIFIED = "identified"
    MITIGATING = "mitigating"
    RESOLVED = "resolved"


class MetricType(str, Enum):
    COUNTER = "counter"
    GAUGE = "gauge"
    HISTOGRAM = "histogram"
    SUMMARY = "summary"


@dataclass(frozen=True)
class ChunkConfig:
    chunk_size: int = 1000
    chunk_overlap: int = 200
    separator: str = "\n\n"
    strategy: Optional[str] = None

    def __post_init__(self) -> None:
        if self.chunk_size < 100 or self.chunk_size > 4000:
            raise ValueError("chunk_size must be between 100 and 4000")
        if self.chunk_overlap < 0 or self.chunk_overlap > 500:
            raise ValueError("chunk_overlap must be between 0 and 500")


@dataclass(frozen=True)
class DocumentMetadata:
    source: str
    title: Optional[str] = None
    author: Optional[str] = None
    created_at: datetime = field(default_factory=datetime.now)
    updated_at: Optional[datetime] = None
    tags: List[str] = field(default_factory=list)
    custom: Dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class Document:
    id: Optional[str] = None
    content: str = ""
    metadata: Optional[DocumentMetadata] = None
    embedding: Optional[List[float]] = None


@dataclass(frozen=True)
class VectorDocument:
    id: Optional[str] = None
    content: str = ""
    embedding: Optional[List[float]] = None
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class PipelineStep:
    name: str
    type: str
    config: Dict[str, Any] = field(default_factory=dict)
    depends_on: List[str] = field(default_factory=list)
    retry: int = 0
    timeout_seconds: int = 300
    retry_policy: Optional[Dict[str, Any]] = None


@dataclass(frozen=True)
class PipelineDefinition:
    name: str
    steps: List[PipelineStep] = field(default_factory=list)
    description: Optional[str] = None
    trigger: Optional[str] = None
    timeout_seconds: int = 3600


@dataclass(frozen=True)
class PipelineRun:
    """Domain value object representing a pipeline execution run."""
    run_id: str
    pipeline_name: str
    status: PipelineStatus = PipelineStatus.PENDING
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    step_results: Dict[str, Any] = field(default_factory=dict)
    error: Optional[str] = None

    @property
    def is_terminal(self) -> bool:
        """Check if the run has reached a terminal state."""
        return self.status in (
            PipelineStatus.SUCCEEDED,
            PipelineStatus.FAILED,
            PipelineStatus.CANCELLED,
        )

    @property
    def duration_seconds(self) -> Optional[float]:
        """Calculate run duration in seconds if completed."""
        if self.started_at and self.completed_at:
            return (self.completed_at - self.started_at).total_seconds()
        return None


@dataclass(frozen=True)
class FeatureDefinition:
    name: str
    dtype: str
    description: Optional[str] = None
    tags: List[str] = field(default_factory=list)


@dataclass(frozen=True)
class FeatureGroup:
    """Feature group definition."""
    name: str
    entities: List[str]
    features: List[FeatureDefinition]
    version: str = "v1"
    description: Optional[str] = None
    online_enabled: bool = True


@dataclass(frozen=True)
class FeatureTransformation:
    """Feature transformation definition."""
    name: str
    input_features: List[str]
    transformation_type: str
    code: Optional[str] = None
    output_type: str = "float"


@dataclass(frozen=True)
class FeatureValue:
    """Single feature value."""
    name: str
    value: Any


@dataclass(frozen=True)
class FeatureMaterializationConfig:
    """Configuration for feature materialization jobs."""
    feature_group: str
    start_time: datetime
    end_time: Optional[datetime] = None
    schedule: Optional[str] = None
    sink: str = "offline"


@dataclass(frozen=True)
class FeatureVector:
    """Feature vector for an entity."""
    entity_id: str
    entity_type: str
    features: List[FeatureValue]
    timestamp: datetime = field(default_factory=datetime.now)
