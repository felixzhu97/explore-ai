"""TTS Service FastAPI Application."""

import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger

from .config import get_settings
from .routers import tts


def get_cors_origins() -> list[str]:
    """Get CORS origins from environment variable or use defaults."""
    origins_env = os.getenv("CORS_ORIGINS", "")
    if origins_env:
        return [origin.strip() for origin in origins_env.split(",") if origin.strip()]
    return [
        "http://localhost:3000",
        "http://localhost:5173",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:5173",
    ]


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler."""
    logger.info("Starting TTS Service...")
    
    # Initialize providers
    try:
        from .providers import get_provider
        settings = get_settings()
        provider = get_provider(settings.tts_provider.value)
        
        if provider.health_check():
            logger.info(f"TTS Provider '{settings.tts_provider.value}' is ready")
        else:
            logger.warning(f"TTS Provider '{settings.tts_provider.value}' health check failed")
    except Exception as e:
        logger.error(f"Failed to initialize TTS provider: {e}")
    
    yield
    
    logger.info("Shutting down TTS Service...")


def create_app() -> FastAPI:
    """Create and configure the FastAPI application."""
    settings = get_settings()
    
    app = FastAPI(
        title="Text-to-Speech Service",
        description="""A unified Text-to-Speech service supporting multiple providers:
        
- **Azure Cognitive Services TTS** - High-quality neural voices, multi-language support
- **Google Cloud Text-to-Speech** - Natural-sounding voices, WaveNet voices
- **ElevenLabs** - Ultra-realistic AI voices with emotion control
- **Coqui TTS (Local)** - Open-source local TTS for privacy-focused deployments
        """,
        version="0.1.0",
        lifespan=lifespan,
        docs_url="/docs",
        redoc_url="/redoc",
    )
    
    # CORS middleware - restrictive settings
    app.add_middleware(
        CORSMiddleware,
        allow_origins=get_cors_origins(),
        allow_credentials=True,
        allow_methods=["GET", "POST", "OPTIONS"],
        allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
    )
    
    # Include routers
    app.include_router(tts.router)
    
    # Root endpoint
    @app.get("/", tags=["Root"])
    async def root():
        """Root endpoint with service information."""
        return {
            "service": "TTS Service",
            "version": "0.1.0",
            "provider": settings.tts_provider.value,
            "docs": "/docs",
            "health": "/tts/health",
            "voices": "/tts/voices",
            "providers": "/tts/providers",
        }
    
    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn
    
    settings = get_settings()
    
    uvicorn.run(
        "src.main:app",
        host=settings.host,
        port=settings.port,
        reload=True,
    )
