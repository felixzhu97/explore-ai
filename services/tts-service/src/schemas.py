"""Pydantic schemas for TTS Service."""

from typing import Optional, List, Dict, Any
from enum import Enum

from pydantic import BaseModel, Field, field_validator


class OutputFormat(str, Enum):
    """Supported audio output formats."""
    MP3 = "mp3"
    WAV = "wav"
    OGG = "ogg"
    FLAC = "flac"


class AudioConfig(BaseModel):
    """Audio configuration settings."""
    sample_rate: Optional[int] = Field(
        default=24000,
        ge=8000,
        le=48000,
        description="Audio sample rate in Hz"
    )
    bit_rate: Optional[int] = Field(
        default=128,
        description="Audio bit rate in kbps"
    )
    channels: int = Field(default=1, description="Number of audio channels")


class SynthesizeRequest(BaseModel):
    """Request model for text synthesis."""
    text: str = Field(
        ...,
        min_length=1,
        max_length=10000,
        description="Text to synthesize"
    )
    voice: Optional[str] = Field(
        default=None,
        description="Voice identifier"
    )
    language: Optional[str] = Field(
        default=None,
        description="Language code (e.g., en-US)"
    )
    speed: float = Field(
        default=1.0,
        ge=0.25,
        le=4.0,
        description="Speech speed multiplier"
    )
    pitch: float = Field(
        default=0,
        ge=-20,
        le=20,
        description="Voice pitch adjustment in Hz"
    )
    output_format: OutputFormat = Field(
        default=OutputFormat.MP3,
        description="Audio output format"
    )
    audio_config: Optional[AudioConfig] = Field(
        default=None,
        description="Audio configuration"
    )
    
    @field_validator('text')
    @classmethod
    def validate_text_not_empty(cls, v: str) -> str:
        """Ensure text is not just whitespace."""
        if not v.strip():
            raise ValueError("Text cannot be empty or whitespace only")
        return v


class StreamRequest(BaseModel):
    """Request model for streaming synthesis."""
    text: str = Field(
        ...,
        min_length=1,
        max_length=10000,
        description="Text to synthesize"
    )
    voice: Optional[str] = Field(default=None, description="Voice identifier")
    language: Optional[str] = Field(default=None, description="Language code")
    speed: float = Field(default=1.0, ge=0.25, le=4.0)
    output_format: OutputFormat = Field(default=OutputFormat.MP3)
    
    @field_validator('text')
    @classmethod
    def validate_text_not_empty(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Text cannot be empty or whitespace only")
        return v


class Voice(BaseModel):
    """Voice information model."""
    id: str = Field(..., description="Voice identifier")
    name: str = Field(..., description="Voice name")
    language: str = Field(..., description="Language code")
    language_name: Optional[str] = Field(None, description="Language display name")
    gender: Optional[str] = Field(None, description="Voice gender")
    provider: str = Field(..., description="TTS provider name")
    is_default: bool = Field(default=False, description="Is default voice")


class ProviderInfo(BaseModel):
    """Provider information model."""
    name: str = Field(..., description="Provider name")
    display_name: str = Field(..., description="Provider display name")
    supported_languages: List[str] = Field(
        default_factory=list,
        description="Supported language codes"
    )
    features: List[str] = Field(
        default_factory=list,
        description="Provider features"
    )


class HealthResponse(BaseModel):
    """Health check response model."""
    status: str = Field(..., description="Service status")
    provider: str = Field(..., description="Active provider")
    provider_status: str = Field(..., description="Provider health status")
    version: str = Field(..., description="Service version")


class ErrorResponse(BaseModel):
    """Error response model."""
    error: str = Field(..., description="Error type")
    message: str = Field(..., description="Error message")
    details: Optional[Dict[str, Any]] = Field(None, description="Additional details")
