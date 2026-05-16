from pydantic_settings import BaseSettings
from functools import lru_cache
from typing import Literal
from pathlib import Path


class Settings(BaseSettings):
    HOST: str = "0.0.0.0"
    PORT: int = 8001
    LOG_LEVEL: str = "INFO"

    QDRANT_HOST: str = "localhost"
    QDRANT_PORT: int = 6333
    QDRANT_COLLECTION: str = "ai_test_docs"
    QDRANT_VECTOR_DIM: int = 384

    EMBEDDING_MODEL: str = "sentence-transformers/all-MiniLM-L6-v2"
    EMBEDDING_DEVICE: str = "cuda"

    LLM_PROVIDER: Literal["openai", "anthropic", "ollama"] = "openai"
    LLM_MODEL: str = "gpt-4o-mini"

    OPENAI_API_KEY: str = ""
    OPENAI_BASE_URL: str = "https://api.openai.com/v1"
    ANTHROPIC_API_KEY: str = ""
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_MODEL: str = "qwen2.5:7b"

    CHUNK_SIZE: int = 500
    CHUNK_OVERLAP: int = 50
    MAX_FILE_SIZE: int = 10 * 1024 * 1024

    # ==================== Persistence Configuration ====================

    # Redis cache configuration (optional, falls back to memory if not configured)
    REDIS_HOST: str = ""
    REDIS_PORT: int = 6379
    REDIS_CACHE_DB: int = 0
    REDIS_SESSION_DB: int = 1

    # Cache TTL configuration (seconds)
    LLM_CACHE_TTL: int = 3600          # LLM response cache: 1 hour
    RETRIEVAL_CACHE_TTL: int = 1800    # Retrieval result cache: 30 minutes
    EMBEDDING_CACHE_TTL: int = 86400   # Embedding cache: 24 hours

    # SQLite database paths
    _db_base_path: str = ""

    @property
    def DB_BASE_PATH(self) -> str:
        if self._db_base_path:
            return self._db_base_path
        return str(Path(__file__).parent.parent.parent / "data")

    @property
    def DOCUMENT_DB_PATH(self) -> str:
        return str(Path(self.DB_BASE_PATH) / "documents.db")

    @property
    def SESSION_DB_PATH(self) -> str:
        return str(Path(self.DB_BASE_PATH) / "sessions.db")

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache
def get_settings() -> Settings:
    return Settings()
