from pydantic import BaseModel, Field
from typing import Optional
from enum import Enum


class DocumentSource(str, Enum):
    MARKDOWN = "markdown"
    PDF = "pdf"
    WEB = "web"
    TEXT = "text"


class DocumentMetadata(BaseModel):
    source: DocumentSource
    filename: Optional[str] = None
    url: Optional[str] = None
    page: Optional[int] = None
    title: Optional[str] = None
    doc_id: Optional[str] = None


class DocumentChunk(BaseModel):
    text: str
    metadata: DocumentMetadata
    chunk_id: int


class UploadResponse(BaseModel):
    doc_id: str
    filename: str
    chunks: int
    status: str


class SourceDocument(BaseModel):
    text: str
    score: float
    metadata: dict


class ChatRequest(BaseModel):
    query: str
    session_id: Optional[str] = None
    doc_ids: Optional[list[str]] = Field(default=None, description="Filter by specific document IDs")
    top_k: int = Field(default=5, ge=1, le=20)
    temperature: float = Field(default=0.7, ge=0, le=2)


class ChatResponse(BaseModel):
    answer: str
    sources: list[SourceDocument]
    session_id: str
    model: str
    processing_time_ms: float


class ChatHistoryItem(BaseModel):
    query: str
    answer: str
    sources: list[SourceDocument]
    timestamp: float


class DocumentStats(BaseModel):
    doc_id: str
    filename: str
    total_chunks: int
    source: str
    uploaded_at: Optional[str] = None


class DocumentListResponse(BaseModel):
    documents: list[DocumentStats]
    total: int


class HealthResponse(BaseModel):
    status: str
    qdrant_connected: bool
    embedding_model: str
    llm_provider: str
