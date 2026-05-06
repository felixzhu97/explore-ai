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
from .api import vision


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting AI Vision Service...")
    yield
    logger.info("Shutting down AI Vision Service...")


app = FastAPI(
    title="AI Vision Service",
    description="Production-grade image recognition API with YOLO, BLIP, and PaddleOCR",
    version="0.1.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(vision.router)


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.get("/")
async def root():
    return {
        "name": "AI Vision Service",
        "version": "0.1.0",
        "endpoints": {
            "health": "/health",
            "detect": "/vision/detect",
            "caption": "/vision/caption",
            "ocr": "/vision/ocr",
            "analyze": "/vision/analyze"
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("src.main:app", host="0.0.0.0", port=8000, reload=True)
