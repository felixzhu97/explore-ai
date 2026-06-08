"""Text-to-Text LLM Service - FastAPI Application.

A unified API service for text generation using multiple LLM providers.
"""

import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger

from src.presentation.api.routes import router
from src.core.config import get_settings


def get_cors_origins() -> list[str]:
    """Get CORS origins from environment variable."""
    origins_env = os.getenv("CORS_ORIGINS", "")
    if origins_env:
        return [origin.strip() for origin in origins_env.split(",") if origin.strip()]
    return [
        "http://localhost:3000",
        "http://localhost:4200",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:4200",
    ]


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler."""
    logger.info("Starting Text-to-Text Service...")
    settings = get_settings()
    logger.info(f"LLM Provider: {settings.LLM_PROVIDER}")
    logger.info(f"Default Model: {settings.LLM_MODEL}")
    logger.info("Text-to-Text Service started (Clean Architecture)")
    yield
    logger.info("Shutting down Text-to-Text Service...")


def create_app() -> FastAPI:
    """Create and configure the FastAPI application."""
    app = FastAPI(
        title="Text-to-Text LLM Service",
        description="A unified API service for text generation using multiple LLM providers.",
        version="0.2.0",
        lifespan=lifespan,
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=get_cors_origins(),
        allow_credentials=True,
        allow_methods=["GET", "POST", "OPTIONS"],
        allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
    )

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
