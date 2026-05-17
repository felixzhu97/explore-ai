"""Text-to-Text LLM Service - FastAPI Application.

A unified API service for text generation using multiple LLM providers.
"""

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger
import sys

from .api.routes import router
from .core.config import get_settings


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

    # CORS middleware
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
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
