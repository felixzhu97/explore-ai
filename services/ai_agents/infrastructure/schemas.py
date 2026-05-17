"""Core schemas for AI Infrastructure Agents.

This module defines Pydantic models for all agent inputs, outputs, and state.
"""

from datetime import datetime
from enum import Enum
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


# ============================================================================
# Enums
# ============================================================================


class DistanceMetric(str, Enum):
    """Distance metric for vector operations."""
    COSINE = "cosine"
    EUCLIDEAN = "euclidean"
    DOT_PRODUCT = "dotproduct"


class PipelineStatus(str, Enum):
    """Status of a pipeline run."""
    PENDING = "pending"
    RUNNING = "running"
    SUCCEEDED = "succeeded"
    FAILED = "failed"
    CANCELLED = "cancelled"


class ModelStage(str, Enum):
    """Stage of a model in its lifecycle."""
    REGISTERED = "registered"
    STAGING = "staging"
    PRODUCTION = "production"
    ARCHIVED = "archived"


class AlertSeverity(str, Enum):
    """Severity level for alerts."""
    CRITICAL = "critical"
    HIGH = "high"
    MEDIUM = "medium"
    LOW = "low"
    INFO = "info"


class IncidentStatus(str, Enum):
    """Status of an incident."""
    OPEN = "open"
    INVESTIGATING = "investigating"
    IDENTIFIED = "identified"
    MITIGATING = "mitigating"
    RESOLVED = "resolved"


class MetricType(str, Enum):
    """Type of metric for monitoring."""
    COUNTER = "counter"
    GAUGE = "gauge"
    HISTOGRAM = "histogram"
    SUMMARY = "summary"


# ============================================================================
# Vector Database Schemas
# ============================================================================


class CollectionConfig(BaseModel):
    """Configuration for a vector collection."""
    name: str = Field(..., description="Collection name")
    dimension: int = Field(..., ge=1, description="Vector dimension")
    distance_metric: DistanceMetric = Field(default=DistanceMetric.COSINE)
    description: Optional[str] = Field(default=None, max_length=500)


class VectorDocument(BaseModel):
    """A document with vector embedding."""
    id: Optional[str] = Field(default=None, description="Document ID")
    content: str = Field(..., description="Document text content")
    embedding: Optional[List[float]] = Field(default=None, description="Vector embedding")
    metadata: Dict[str, Any] = Field(default_factory=dict)


class VectorQuery(BaseModel):
    """Query parameters for vector search."""
    collection: str = Field(..., description="Target collection")
    query_embedding: List[float] = Field(..., description="Query vector")
    top_k: int = Field(default=5, ge=1, le=100)
    filters: Optional[Dict[str, Any]] = Field(default=None)


# ============================================================================
# RAG Agent Schemas
# ============================================================================


class DocumentMetadata(BaseModel):
    """Metadata for indexed documents."""
    source: str = Field(..., description="Document source")
    title: Optional[str] = Field(default=None)
    author: Optional[str] = Field(default=None)
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: Optional[datetime] = None
    tags: List[str] = Field(default_factory=list)
    custom: Dict[str, Any] = Field(default_factory=dict)


class Document(BaseModel):
    """A document for RAG operations."""
    id: Optional[str] = Field(default=None)
    content: str = Field(..., description="Document content")
    metadata: DocumentMetadata = Field(default_factory=DocumentMetadata)
    embedding: Optional[List[float]] = None


class ChunkConfig(BaseModel):
    """Configuration for document chunking."""
    chunk_size: int = Field(default=1000, ge=100, le=4000)
    chunk_overlap: int = Field(default=200, ge=0, le=500)
    separator: str = Field(default="\n\n")


class RAGQuery(BaseModel):
    """Query for RAG retrieval."""
    query: str = Field(..., description="User query")
    collection: str = Field(..., description="Target collection")
    top_k: int = Field(default=5, ge=1, le=20)
    filters: Optional[Dict[str, Any]] = Field(default=None)
    rerank: bool = Field(default=True, description="Enable reranking")


class RAGResult(BaseModel):
    """Result from RAG retrieval."""
    content: str
    score: float
    metadata: DocumentMetadata
    source: str


class RAGResponse(BaseModel):
    """Response from RAG query."""
    results: List[RAGResult]
    query: str
    total_results: int
    generation_kwargs: Dict[str, Any] = Field(default_factory=dict)


# ============================================================================
# LLMOps Agent Schemas
# ============================================================================


class ModelVersion(BaseModel):
    """A version of a registered model."""
    version: str = Field(..., pattern=r"^v\d+\.\d+\.\d+$")
    created_at: datetime = Field(default_factory=datetime.now)
    description: Optional[str] = None
    metrics: Dict[str, float] = Field(default_factory=dict)
    artifacts: Dict[str, str] = Field(default_factory=dict)
    status: ModelStage = Field(default=ModelStage.REGISTERED)


class ModelRegistration(BaseModel):
    """Registration payload for a new model."""
    name: str = Field(..., min_length=1, max_length=100)
    description: Optional[str] = None
    framework: str = Field(default="pytorch", description="ML framework")
    metadata: Dict[str, Any] = Field(default_factory=dict)


class TrainingConfig(BaseModel):
    """Configuration for model training."""
    model_name: str = Field(..., description="Model to train")
    dataset: str = Field(..., description="Training dataset")
    epochs: int = Field(default=10, ge=1, le=1000)
    batch_size: int = Field(default=32, ge=1)
    learning_rate: float = Field(default=0.001, gt=0)
    optimizer: str = Field(default="adam")
    scheduler: Optional[str] = None
    early_stopping: bool = Field(default=True)
    validation_split: float = Field(default=0.2, ge=0.0, le=0.5)


class TrainingResult(BaseModel):
    """Result from a training run."""
    run_id: str
    status: PipelineStatus
    metrics: Dict[str, float]
    duration_seconds: float
    artifacts: Dict[str, str] = Field(default_factory=dict)


class EvaluationConfig(BaseModel):
    """Configuration for model evaluation."""
    model_version: str
    dataset: str
    metrics: List[str] = Field(
        default=["accuracy", "precision", "recall", "f1"]
    )
    batch_size: int = Field(default=32)


class EvaluationResult(BaseModel):
    """Result from model evaluation."""
    model_version: str
    metrics: Dict[str, float]
    timestamp: datetime = Field(default_factory=datetime.now)
    dataset: str
    predictions: Optional[List[Any]] = None


class DeploymentConfig(BaseModel):
    """Configuration for model deployment."""
    model_name: str
    version: str
    replicas: int = Field(default=1, ge=1)
    resources: Dict[str, Any] = Field(
        default_factory=lambda: {"cpu": "1", "memory": "2Gi"}
    )
    strategy: str = Field(default="rolling")  # rolling, blue_green, canary


class TrafficSplit(BaseModel):
    """Traffic split configuration for A/B testing."""
    splits: Dict[str, float] = Field(
        ...,
        description="Model version to traffic percentage mapping"
    )
    duration: str = Field(default="24h")
    success_metric: str = Field(default="accuracy")


# ============================================================================
# Feature Store Agent Schemas
# ============================================================================


class FeatureDefinition(BaseModel):
    """Definition of a feature."""
    name: str = Field(..., pattern=r"^[a-z][a-z0-9_]*$")
    dtype: str = Field(..., description="Data type: int, float, str, bool")
    description: Optional[str] = None
    tags: List[str] = Field(default_factory=list)


class FeatureGroup(BaseModel):
    """A group of related features."""
    name: str = Field(..., pattern=r"^[a-z][a-z0-9_]*$")
    version: str = Field(default="1")
    entities: List[str] = Field(..., description="Entity identifiers")
    features: List[FeatureDefinition] = Field(...)
    description: Optional[str] = None
    online_enabled: bool = Field(default=True)


class FeatureValue(BaseModel):
    """A single feature value."""
    name: str
    value: Any


class FeatureVector(BaseModel):
    """A vector of feature values for an entity."""
    entity_id: str
    entity_type: str
    features: List[FeatureValue]
    timestamp: datetime = Field(default_factory=datetime.now)


class FeatureMaterializationConfig(BaseModel):
    """Configuration for feature materialization."""
    feature_group: str
    start_time: datetime
    end_time: Optional[datetime] = None
    schedule: Optional[str] = None  # cron expression
    sink: str = Field(default="offline")  # offline, online, both


class FeatureTransformation(BaseModel):
    """Definition of a feature transformation."""
    name: str
    input_features: List[str]
    transformation_type: str  # sql, python, udf
    code: Optional[str] = None
    output_type: str


# ============================================================================
# Pipeline Agent Schemas
# ============================================================================


class PipelineDefinition(BaseModel):
    """Definition of a data/ML pipeline."""
    name: str = Field(..., pattern=r"^[a-z][a-z0-9_-]*$")
    description: Optional[str] = None
    steps: List["PipelineStep"] = Field(...)
    trigger: Optional[str] = None  # cron, webhook, manual
    timeout_seconds: int = Field(default=3600)


class PipelineStep(BaseModel):
    """A single step in a pipeline."""
    name: str
    type: str  # data_processing, model_training, validation, notification
    config: Dict[str, Any] = Field(default_factory=dict)
    depends_on: List[str] = Field(default_factory=list)
    retry: int = Field(default=0, ge=0, le=5)
    timeout_seconds: int = Field(default=300)


class PipelineRun(BaseModel):
    """A single execution of a pipeline."""
    run_id: str
    pipeline_name: str
    status: PipelineStatus = Field(default=PipelineStatus.PENDING)
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    step_results: Dict[str, Any] = Field(default_factory=dict)
    error: Optional[str] = None


class PipelineExecution(BaseModel):
    """Execution plan for a pipeline."""
    pipeline: PipelineDefinition
    run: PipelineRun
    next_steps: List[str] = Field(default_factory=list)


# ============================================================================
# AIOps Agent Schemas
# ============================================================================


class TimeSeriesPoint(BaseModel):
    """A single point in a time series."""
    timestamp: datetime
    value: float


class TimeSeriesData(BaseModel):
    """Time series data for analysis."""
    metric_name: str
    points: List[TimeSeriesPoint]
    labels: Dict[str, str] = Field(default_factory=dict)


class AnomalyDetectionConfig(BaseModel):
    """Configuration for anomaly detection."""
    metric: str
    time_range: str = Field(default="1h")
    sensitivity: float = Field(default=0.5, ge=0.0, le=1.0)
    algorithm: str = Field(default="isolation_forest")  # isolation_forest, mad, zscore


class AnomalyResult(BaseModel):
    """Result from anomaly detection."""
    is_anomaly: bool
    score: float
    threshold: float
    explanation: str
    timestamp: datetime = Field(default_factory=datetime.now)


class RootCauseAnalysisRequest(BaseModel):
    """Request for root cause analysis."""
    incident_id: str
    time_range: str = Field(default="1h")
    affected_services: List[str]
    include_related_logs: bool = Field(default=True)


class RootCauseResult(BaseModel):
    """Result from root cause analysis."""
    incident_id: str
    root_cause: str
    confidence: float
    contributing_factors: List[str]
    evidence: Dict[str, Any] = Field(default_factory=dict)
    recommendations: List[str]


class IncidentCreateRequest(BaseModel):
    """Request to create an incident."""
    title: str
    severity: AlertSeverity
    description: str
    affected_systems: List[str]
    labels: Dict[str, str] = Field(default_factory=dict)


class IncidentResponse(BaseModel):
    """Response containing incident details."""
    incident_id: str
    status: IncidentStatus
    title: str
    severity: AlertSeverity
    description: str
    affected_systems: List[str]
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)
    assignee: Optional[str] = None
    timeline: List[Dict[str, Any]] = Field(default_factory=list)


class LogEntry(BaseModel):
    """A single log entry."""
    timestamp: datetime
    level: str  # DEBUG, INFO, WARNING, ERROR, CRITICAL
    message: str
    service: str
    labels: Dict[str, str] = Field(default_factory=dict)


class LogQuery(BaseModel):
    """Query for log search."""
    query: str
    time_range: str = Field(default="1h")
    limit: int = Field(default=100, ge=1, le=1000)
    filters: Dict[str, str] = Field(default_factory=dict)


class MetricQuery(BaseModel):
    """Query for metric data."""
    metric: str
    time_range: str = Field(default="5m")
    aggregation: str = Field(default="avg")  # avg, sum, min, max
    labels: Dict[str, str] = Field(default_factory=dict)


# ============================================================================
# Update forward references
# ============================================================================

PipelineDefinition.model_rebuild()
