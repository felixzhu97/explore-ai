"""Pydantic schemas for Text-to-Text API."""

from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any, Union


class Message(BaseModel):
    """Chat message schema."""
    role: str = Field(default="user", description="Message role (user/assistant/system)")
    content: str = Field(..., description="Message content")


class CompletionRequest(BaseModel):
    """Text completion request schema."""
    prompt: str = Field(..., description="Input prompt for text generation")
    system_prompt: Optional[str] = Field(None, description="Optional system prompt")
    provider: Optional[str] = Field(None, description="LLM provider override")
    model: Optional[str] = Field(None, description="Model name override")
    temperature: float = Field(default=0.7, ge=0.0, le=2.0, description="Sampling temperature")
    max_tokens: int = Field(default=4096, ge=1, le=32768, description="Maximum tokens to generate")


class CompletionResponse(BaseModel):
    """Text completion response schema."""
    text: str = Field(..., description="Generated text")
    provider: str = Field(..., description="LLM provider used")
    model: str = Field(..., description="Model used")
    usage: Optional[Dict[str, int]] = Field(None, description="Token usage information")
    finish_reason: Optional[str] = Field(None, description="Reason for completion")


class ChatRequest(BaseModel):
    """Chat completion request schema."""
    messages: List[Message] = Field(..., description="Conversation messages")
    system_prompt: Optional[str] = Field(None, description="Optional system prompt override")
    provider: Optional[str] = Field(None, description="LLM provider override")
    model: Optional[str] = Field(None, description="Model name override")
    temperature: float = Field(default=0.7, ge=0.0, le=2.0, description="Sampling temperature")
    max_tokens: int = Field(default=4096, ge=1, le=32768, description="Maximum tokens to generate")
    session_id: Optional[str] = Field(None, description="Optional session ID for conversation context")


class ChatResponse(BaseModel):
    """Chat completion response schema."""
    text: str = Field(..., description="Generated response text")
    provider: str = Field(..., description="LLM provider used")
    model: str = Field(..., description="Model used")
    session_id: str = Field(..., description="Session ID")
    usage: Optional[Dict[str, int]] = Field(None, description="Token usage information")
    finish_reason: Optional[str] = Field(None, description="Reason for completion")


class ModelInfo(BaseModel):
    """Model information schema."""
    name: str = Field(..., description="Model name")
    provider: str = Field(..., description="Provider name")
    description: Optional[str] = Field(None, description="Model description")
    max_tokens: Optional[int] = Field(None, description="Maximum context length")


class ProviderInfo(BaseModel):
    """Provider information schema."""
    name: str = Field(..., description="Provider name")
    display_name: str = Field(..., description="Human-readable name")
    models: List[str] = Field(..., description="Available models")
    status: str = Field(default="available", description="Provider status")


class HealthResponse(BaseModel):
    """Health check response schema."""
    status: str = Field(..., description="Service status")
    provider: str = Field(..., description="Current LLM provider")
    model: str = Field(..., description="Current model")
    version: str = Field(..., description="Service version")
