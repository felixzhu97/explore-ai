import os
import sys
from pathlib import Path

# Ensure src directory is in the path for lazy imports
_src_path = Path(__file__).parent.resolve()
if str(_src_path) not in sys.path:
    sys.path.insert(0, str(_src_path))

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from loguru import logger

from .api.documents import router as documents_router
from .api.chat import router as chat_router
from .config import get_settings
from .core.vector_store import VectorStore, reset_vector_store
from .core.embedding import EmbeddingModel
from .core.llm_gateway import LLMGateway
from .schemas import HealthResponse
from .persistence.cache_manager import get_cache_manager, reset_cache_manager
from .persistence.document_metadata import get_document_store
from .persistence.session_store import get_session_store


def get_cors_origins() -> list[str]:
    """Get CORS origins from environment variable or use defaults."""
    origins_env = os.getenv("CORS_ORIGINS", "")
    if origins_env:
        return [origin.strip() for origin in origins_env.split(",") if origin.strip()]
    return [
        "http://localhost:3000",
        "http://localhost:4200",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:4200",
    ]


_vector_store: VectorStore = None
_embedding_model: EmbeddingModel = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _vector_store, _embedding_model

    settings = get_settings()
    logger.info("Starting RAG Service...")

    try:
        _vector_store = VectorStore()
        _vector_store.create_collection()
        logger.info("Qdrant vector store initialized")
    except Exception as e:
        logger.warning(f"Could not initialize Qdrant: {e}")

    try:
        _embedding_model = EmbeddingModel()
        logger.info("Embedding model initialized")
    except Exception as e:
        logger.warning(f"Could not initialize embedding model: {e}")

    # Initialize persistence components
    try:
        get_cache_manager()
        logger.info("Cache manager initialized")
    except Exception as e:
        logger.warning(f"Could not initialize cache manager: {e}")

    try:
        get_document_store()
        logger.info("Document metadata store initialized")
    except Exception as e:
        logger.warning(f"Could not initialize document store: {e}")

    try:
        get_session_store()
        logger.info("Session store initialized")
    except Exception as e:
        logger.warning(f"Could not initialize session store: {e}")

    logger.info(f"RAG Service started on {settings.HOST}:{settings.PORT}")

    yield

    logger.info("Shutting down RAG Service...")
    if _vector_store:
        _vector_store.close()


def create_app() -> FastAPI:
    app = FastAPI(
        title="RAG Service",
        description="Production RAG service with Qdrant vector store",
        version="0.2.0",
        lifespan=lifespan,
    )

    # CORS middleware - restrictive settings
    app.add_middleware(
        CORSMiddleware,
        allow_origins=get_cors_origins(),
        allow_credentials=True,
        allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
        allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
    )

    app.include_router(documents_router)
    app.include_router(chat_router)

    @app.get("/health", response_model=HealthResponse)
    async def health():
        settings = get_settings()
        qdrant_connected = False
        current_model = settings.OLLAMA_MODEL if settings.LLM_PROVIDER == "ollama" else settings.LLM_MODEL

        try:
            if _vector_store and _vector_store.client:
                _vector_store.client.get_collection(collection_name=_vector_store.collection_name)
                qdrant_connected = True
        except Exception:
            pass

        return HealthResponse(
            status="ok" if qdrant_connected else "degraded",
            qdrant_connected=qdrant_connected,
            embedding_model=settings.EMBEDDING_MODEL,
            llm_provider=settings.LLM_PROVIDER,
        )

    @app.get("/")
    async def root():
        settings = get_settings()
        current_model = settings.OLLAMA_MODEL if settings.LLM_PROVIDER == "ollama" else settings.LLM_MODEL
        return {
            "name": "RAG Service",
            "version": "0.2.0",
            "description": "Production RAG service with Qdrant vector store",
            "endpoints": {
                "health": "/health",
                "documents": {
                    "upload": "POST /api/rag/documents/upload",
                    "ingest_url": "POST /api/rag/documents/ingest-url",
                    "list": "GET /api/rag/documents/",
                    "list_from_db": "GET /api/rag/documents/database",
                    "stats": "GET /api/rag/documents/{doc_id}/stats",
                    "delete": "DELETE /api/rag/documents/{doc_id}",
                },
                "chat": {
                    "query": "POST /api/rag/chat/",
                    "stream": "POST /api/rag/chat/stream",
                    "history": "GET /api/rag/chat/history/{session_id}",
                    "ingest_text": "POST /api/rag/chat/ingest-text",
                },
                "reload": "POST /reload",
                "cache": {
                    "stats": "GET /cache/stats",
                    "clear": "POST /cache/clear",
                },
            },
            "config": {
                "llm_provider": settings.LLM_PROVIDER,
                "llm_model": current_model,
                "embedding_model": settings.EMBEDDING_MODEL,
            },
        }

    @app.post("/reload")
    async def reload_config():
        """Reload configuration from .env file."""
        get_settings.cache_clear()
        reset_vector_store()
        reset_cache_manager()
        LLMGateway.reset()
        settings = get_settings()
        current_model = settings.OLLAMA_MODEL if settings.LLM_PROVIDER == "ollama" else settings.LLM_MODEL
        return {
            "status": "success",
            "config": {
                "llm_provider": settings.LLM_PROVIDER,
                "llm_model": current_model,
                "embedding_model": settings.EMBEDDING_MODEL,
            }
        }

    @app.get("/cache/stats")
    async def cache_stats():
        """Get cache statistics."""
        cache_manager = get_cache_manager()
        doc_store = get_document_store()
        session_store = get_session_store()

        return {
            "cache": cache_manager.get_stats(),
            "documents": doc_store.get_stats(),
            "sessions": session_store.get_stats(),
        }

    @app.post("/cache/clear")
    async def clear_cache():
        """Clear all caches."""
        reset_cache_manager()
        return {"status": "success", "message": "Cache cleared"}

    return app


app = create_app()


if __name__ == "__main__":
    import uvicorn

    settings = get_settings()
    uvicorn.run(
        "src.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=True,
    )
