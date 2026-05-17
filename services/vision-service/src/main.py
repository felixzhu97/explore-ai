import os

# Set HF mirror at startup (must be before importing transformers)
if not os.environ.get("HF_ENDPOINT"):
    try:
        from .core.config import get_settings
        settings = get_settings()
        if settings.HF_ENDPOINT:
            os.environ["HF_ENDPOINT"] = settings.HF_ENDPOINT
    except Exception:
        pass

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from loguru import logger
from .api import vision, video, image_gen


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
    logger.info("Starting AI Vision Service...")
    yield
    logger.info("Shutting down AI Vision Service...")


app = FastAPI(
    title="AI Vision Service",
    description="Production-grade image recognition and generation API with YOLO, BLIP, PaddleOCR, Stable Diffusion, and Video Generation",
    version="0.2.0",
    lifespan=lifespan
)

# CORS middleware - restrictive settings
app.add_middleware(
    CORSMiddleware,
    allow_origins=get_cors_origins(),
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "X-Request-ID"],
)

app.include_router(vision.router)
app.include_router(video.router)
app.include_router(image_gen.router)


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.get("/")
async def root():
    return {
        "name": "AI Vision Service",
        "version": "0.2.0",
        "capabilities": {
            "vision": ["object_detection", "image_captioning", "ocr"],
            "image_generation": ["text_to_image", "variation", "upscale"],
            "video": ["text_to_video", "image_to_video"]
        },
        "endpoints": {
            "health": "/health",
            "vision": {
                "detect": "/vision/detect",
                "caption": "/vision/caption",
                "ocr": "/vision/ocr",
                "analyze": "/vision/analyze"
            },
            "image_generation": {
                "generate": "/image-gen/generate",
                "variation": "/image-gen/variation",
                "upscale": "/image-gen/upscale",
                "models": "/image-gen/models",
                "cache_clear": "/image-gen/cache/clear",
                "health": "/image-gen/health"
            },
            "video": {
                "generate": "/video/generate",
                "status": "/video/status/{task_id}"
            }
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("src.main:app", host="0.0.0.0", port=8000, reload=True)
