"""Application settings from environment variables."""
from __future__ import annotations

import os
from typing import Optional

from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    """Application settings."""

    # Server
    port: int = 8015
    host: str = "0.0.0.0"

    # Model
    sd_model: str = "runwayml/stable-diffusion-v1-5"
    device: str = "auto"

    # HuggingFace
    hf_endpoint: Optional[str] = None
    hf_token: Optional[str] = None

    # CORS
    cors_origins: str = "http://localhost:3000,http://localhost:4200"

    class Config:
        env_prefix = "MEDIA_GEN_"
        case_sensitive = False
        extra = "ignore"


@lru_cache
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()
