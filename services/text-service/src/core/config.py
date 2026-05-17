"""Configuration for Text-to-Text service."""

from pydantic_settings import BaseSettings
from functools import lru_cache
from typing import Literal


class Settings(BaseSettings):
    """Application settings with environment variable support."""

    HOST: str = "0.0.0.0"
    PORT: int = 8006
    LOG_LEVEL: str = "INFO"

    # Default LLM Provider
    LLM_PROVIDER: Literal["openai", "anthropic", "ollama"] = "openai"
    LLM_MODEL: str = "gpt-4o-mini"
    LLM_TEMPERATURE: float = 0.7
    LLM_MAX_TOKENS: int = 4096

    # OpenAI Configuration
    OPENAI_API_KEY: str = ""
    OPENAI_BASE_URL: str = "https://api.openai.com/v1"
    OPENAI_TIMEOUT: int = 120

    # Anthropic Configuration
    ANTHROPIC_API_KEY: str = ""
    ANTHROPIC_TIMEOUT: int = 120

    # Ollama Configuration (Local)
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "llama3.3:latest"
    OLLAMA_TIMEOUT: int = 300

    # Available Models by Provider
    OPENAI_MODELS: str = "gpt-4o,gpt-4o-mini,gpt-4-turbo,gpt-3.5-turbo"
    ANTHROPIC_MODELS: str = "claude-sonnet-4-20250514,claude-opus-4-20250514,claude-3-5-sonnet-20241022"
    OLLAMA_MODELS: str = "llama3.3:latest,deepseek-r1:70b,qwen3.5:35b,qwen3-coder:30b"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()
