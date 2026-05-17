"""Text-to-Text LLM Service - FastAPI Application.

A unified API service for text generation using multiple LLM providers.
"""

import os
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from loguru import logger
import sys

from .api.routes import router
from .core.config import get_settings


def get_cors_origins() -> list[str]:
    """Get CORS origins from environment variable or use defaults.
    
    In production, set CORS_ORIGINS to comma-separated list of allowed origins.
    Default to localhost for development only.
    """
    origins_env = os.getenv("CORS_ORIGINS", "")
    if origins_env:
        return [origin.strip() for origin in origins_env.split(",") if origin.strip()]
    
    # Development defaults (restrictive for production use)
    return [
        "http://localhost:3000",
        "http://localhost:5173",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:5173",
    ]


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler."""
    logger.info("Starting Text-to-Text Service...")
    settings = get_settings()
    logger.info(f"LLM Provider: {settings.LLM_PROVIDER}")
    logger.info(f"Default Model: {settings.LLM_MODEL}")
    logger.info("Text-to-Text Service started")
    yield
    logger.info("Shutting down Text-to-Text Service...")


def create_app() -> FastAPI:
    """Create and configure the FastAPI application."""
    app = FastAPI(
        title="Text-to-Text LLM Service",
        description="""
## Overview

A unified API service for text generation using multiple LLM providers.

### Supported Providers

- **OpenAI**: GPT-4o, GPT-4o-mini, GPT-4-turbo, GPT-3.5-turbo
- **Anthropic**: Claude Sonnet, Claude Opus
- **Ollama**: Local models (qwen2.5, llama3.2, mistral)

### Features

- Text completion
- Chat completion with session management
- Streaming responses (SSE)
- Multi-provider support
- Configurable temperature and max tokens
        """,
        version="0.1.0",
        lifespan=lifespan,
    )

    # CORS middleware - restrictive settings
    app.add_middleware(
        CORSMiddleware,
        allow_origins=get_cors_origins(),
        allow_credentials=True,
        allow_methods=["GET", "POST", "OPTIONS"],
        allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
    )

    # Include API routes
    app.include_router(router)

    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn
    settings = get_settings()
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=True,
        log_level=settings.LOG_LEVEL.lower(),
    )
