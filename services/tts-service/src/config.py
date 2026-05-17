"""Configuration management for TTS Service."""

from enum import Enum
from typing import Optional
from functools import lru_cache

from pydantic_settings import BaseSettings
from pydantic import Field


class TTSProvider(str, Enum):
    """Supported TTS providers."""
    AZURE = "azure"
    GOOGLE = "google"
    ELEVENLABS = "elevenlabs"
    COQUI = "coqui"
    EDGE = "edge"


class Settings(BaseSettings):
    """TTS Service configuration settings."""
    
    # Provider configuration
    tts_provider: TTSProvider = Field(
        default=TTSProvider.EDGE,
        description="Active TTS provider"
    )
    
    # Azure TTS
    azure_speech_key: Optional[str] = Field(
        default=None,
        description="Azure Cognitive Services subscription key"
    )
    azure_speech_region: str = Field(
        default="eastus",
        description="Azure region for TTS"
    )
    
    # Google TTS
    google_application_credentials: Optional[str] = Field(
        default=None,
        description="Path to GCP service account JSON"
    )
    
    # ElevenLabs
    elevenlabs_api_key: Optional[str] = Field(
        default=None,
        description="ElevenLabs API key"
    )
    
    # Coqui TTS
    coqui_model_path: Optional[str] = Field(
        default=None,
        description="Path to Coqui TTS model"
    )
    
    # Default voice settings
    default_voice: str = Field(
        default="en-US-JennyNeural",
        description="Default voice identifier"
    )
    default_language: str = Field(
        default="en-US",
        description="Default language code"
    )
    default_speed: float = Field(
        default=1.0,
        ge=0.25,
        le=4.0,
        description="Speech speed multiplier"
    )
    default_pitch: float = Field(
        default=0,
        ge=-20,
        le=20,
        description="Voice pitch adjustment in Hz"
    )
    
    # Server settings
    host: str = Field(default="0.0.0.0", description="Server host")
    port: int = Field(default=8013, description="Server port")
    
    # Cache settings
    enable_cache: bool = Field(default=True, description="Enable audio caching")
    cache_ttl: int = Field(default=3600, description="Cache TTL in seconds")
    
    class Config:
        env_file = ".env"
        env_prefix = "TTS_"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()
