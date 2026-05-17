"""Media Generation Service - Local Text-to-Image using Stable Diffusion."""

import io
import os
import base64
import time
import torch
from typing import Optional
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from dotenv import load_dotenv
from loguru import logger

load_dotenv()

# Configuration
MEDIA_GEN_PORT = int(os.getenv("MEDIA_GEN_PORT", "3456"))
SD_MODEL = os.getenv("SD_MODEL", "runwayml/stable-diffusion-v1-5")
DEVICE_CONFIG = os.getenv("MEDIA_GEN_DEVICE", "auto")

# Device selection logic
def get_device() -> str:
    if DEVICE_CONFIG == "auto":
        if torch.cuda.is_available():
            return "cuda"
        elif hasattr(torch.backends, "mps") and torch.backends.mps.is_available():
            return "mps"
        return "cpu"
    return DEVICE_CONFIG

HF_ENDPOINT = os.getenv("HF_ENDPOINT")  # e.g. https://hf-mirror.com for China
HF_TOKEN = os.getenv("HF_TOKEN")

# Set HF endpoint if mirror is configured
if HF_ENDPOINT:
    os.environ["HF_ENDPOINT"] = HF_ENDPOINT

# Global pipeline reference
_pipeline = None


def get_pipeline():
    """Lazy-load the Stable Diffusion pipeline."""
    global _pipeline
    if _pipeline is None:
        from diffusers import StableDiffusionPipeline
        
        device = get_device()
        logger.info(f"Loading SD model '{SD_MODEL}' on device: {device}")
        
        start_time = time.time()
        load_kwargs = {
            "torch_dtype": torch.float16 if device == "cuda" else torch.float32,
            "safety_checker": None,
        }
        if HF_TOKEN:
            load_kwargs["use_auth_token"] = HF_TOKEN
        
        _pipeline = StableDiffusionPipeline.from_pretrained(
            SD_MODEL,
            **load_kwargs,
        )
        _pipeline = _pipeline.to(device)
        
        if hasattr(_pipeline, "enable_attention_slicing"):
            _pipeline.enable_attention_slicing()
        
        elapsed = time.time() - start_time
        logger.info(f"Model loaded in {elapsed:.1f}s")
    
    return _pipeline


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler."""
    logger.info("Starting Media Generation Service...")
    logger.info(f"Model: {SD_MODEL}")
    logger.info(f"Device: {get_device()}")
    
    yield
    
    logger.info("Shutting down Media Generation Service...")


app = FastAPI(
    title="Media Generation Service",
    description="Local Text-to-Image generation using Stable Diffusion",
    version="0.1.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class ImageGenerationRequest(BaseModel):
    prompt: str = Field(..., min_length=1, max_length=1000, description="Text prompt for image generation")
    negative_prompt: str = Field(
        default="blurry, ugly, distorted, low quality, watermark, text, signature",
        max_length=500
    )
    width: int = Field(default=512, ge=256, le=1024)
    height: int = Field(default=512, ge=256, le=1024)
    num_inference_steps: int = Field(default=25, ge=1, le=100)
    guidance_scale: float = Field(default=7.5, ge=1.0, le=20.0)
    seed: Optional[int] = Field(default=None, ge=0)
    num_images: int = Field(default=1, ge=1, le=4)


class ImageGenerationResponse(BaseModel):
    images: list[str]  # Base64 encoded images
    seed: int
    model: str
    prompt: str
    width: int
    height: int
    num_inference_steps: int
    guidance_scale: float
    processing_time_ms: float


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    device = get_device()
    return {
        "status": "ok",
        "model": SD_MODEL,
        "device": device,
        "cuda_available": torch.cuda.is_available(),
        "mps_available": hasattr(torch.backends, "mps") and torch.backends.mps.is_available(),
        "cuda_device_count": torch.cuda.device_count() if torch.cuda.is_available() else 0,
        "pipeline_loaded": _pipeline is not None,
    }


@app.post("/image/generate", response_model=ImageGenerationResponse)
async def generate_image(request: ImageGenerationRequest, background_tasks: BackgroundTasks):
    """
    Generate images from text prompt using Stable Diffusion.
    
    Returns base64-encoded PNG images.
    """
    start_time = time.time()
    
    try:
        pipeline = get_pipeline()
        device = get_device()
        
        # Set seed for reproducibility
        generator_seed = request.seed if request.seed is not None else torch.randint(
            0, 2**32 - 1, (1,)
        ).item()
        generator = torch.Generator(device=device).manual_seed(generator_seed)
        
        logger.info(f"Generating image: '{request.prompt[:50]}...' (seed: {generator_seed})")
        
        # Generate image(s)
        result = pipeline(
            prompt=request.prompt,
            negative_prompt=request.negative_prompt,
            width=request.width,
            height=request.height,
            num_inference_steps=request.num_inference_steps,
            guidance_scale=request.guidance_scale,
            generator=generator,
            num_images_per_prompt=request.num_images,
        )
        
        # Encode images to base64
        images_b64 = []
        for img in result.images:
            buffered = io.BytesIO()
            img.save(buffered, format="PNG")
            img_b64 = base64.b64encode(buffered.getvalue()).decode("utf-8")
            images_b64.append(img_b64)
        
        processing_time = (time.time() - start_time) * 1000
        logger.info(f"Generated {len(images_b64)} image(s) in {processing_time:.0f}ms")
        
        return ImageGenerationResponse(
            images=images_b64,
            seed=generator_seed,
            model=SD_MODEL,
            prompt=request.prompt,
            width=request.width,
            height=request.height,
            num_inference_steps=request.num_inference_steps,
            guidance_scale=request.guidance_scale,
            processing_time_ms=processing_time,
        )
    
    except Exception as e:
        logger.error(f"Image generation failed: {e}")
        raise HTTPException(status_code=500, detail=f"Generation failed: {str(e)}")


@app.post("/cache/clear")
async def clear_cache():
    """Clear the model cache to free memory."""
    global _pipeline
    if _pipeline is not None:
        del _pipeline
        _pipeline = None
    
    if torch.cuda.is_available():
        torch.cuda.empty_cache()
    
    return {"status": "ok", "message": "Cache cleared successfully"}


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": "Media Generation Service",
        "version": "0.1.0",
        "model": SD_MODEL,
        "endpoints": {
            "health": "/health",
            "generate": "/image/generate",
            "clear_cache": "/cache/clear",
            "docs": "/docs",
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app:app", host="0.0.0.0", port=MEDIA_GEN_PORT, reload=False)
