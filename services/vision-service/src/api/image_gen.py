from fastapi import APIRouter, HTTPException, Depends
from typing import Optional
import asyncio
import logging
from concurrent.futures import ThreadPoolExecutor

from ..schemas.image_gen import (
    ImageGenRequest, ImageGenResponse,
    ImageVariationRequest, ImageUpscaleRequest,
    AvailableModelsResponse
)
from ..models.text_to_image import TextToImageGenerator, get_generator
from ..core.image_gen_config import get_image_gen_settings

router = APIRouter(prefix="/image-gen", tags=["image-generation"])
logger = logging.getLogger(__name__)

_executor = ThreadPoolExecutor(max_workers=2)


def get_generator_dep() -> TextToImageGenerator:
    """Dependency for getting the generator instance."""
    return get_generator()


@router.post("/generate", response_model=ImageGenResponse)
async def generate_image(
    request: ImageGenRequest,
    generator: TextToImageGenerator = Depends(get_generator_dep)
):
    """
    Generate images from text prompt using Stable Diffusion.
    
    Supports multiple models:
    - SDXL (default): Fast, good quality
    - SD3: Higher quality, better faces and composition
    
    **Parameters:**
    - prompt: Text description of the desired image (required)
    - negative_prompt: Things to avoid in the image
    - model: Model to use (sdxl, sd3)
    - width/height: Output dimensions (256-2048, must be divisible by 8)
    - num_inference_steps: Quality vs speed tradeoff (1-150)
    - guidance_scale: How closely to follow the prompt (1-20)
    - seed: For reproducible results (optional)
    - num_images: Number of images to generate (1-4)
    - style_preset: Optional style (photograph, digital-art, etc.)
    
    **Returns:**
    - images: List of base64-encoded PNG images
    - metadata: Generation parameters and timing info
    """
    if not request.prompt.strip():
        raise HTTPException(400, "Prompt cannot be empty")
    
    settings = get_image_gen_settings()
    
    if request.width > settings.MAX_IMAGE_SIZE or request.height > settings.MAX_IMAGE_SIZE:
        raise HTTPException(
            400, 
            f"Image dimensions cannot exceed {settings.MAX_IMAGE_SIZE}px"
        )
    
    if request.num_inference_steps > 150:
        raise HTTPException(400, "Maximum 150 inference steps allowed")

    try:
        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(
            _executor,
            generator.generate,
            request
        )
        return result
    except Exception as e:
        logger.error(f"Image generation failed: {e}")
        raise HTTPException(500, f"Generation failed: {str(e)}")


@router.post("/variation", response_model=ImageGenResponse)
async def generate_variation(
    request: ImageVariationRequest,
    generator: TextToImageGenerator = Depends(get_generator_dep)
):
    """
    Generate variations of an existing image.
    
    The variation maintains the overall composition while applying
    the style/interpretation from the prompt.
    
    **Parameters:**
    - image: Base64-encoded source image
    - prompt: Description of the desired variation
    - strength: How much to transform (0.0-1.0, lower = more faithful)
    - num_inference_steps: Quality vs speed (1-150)
    - guidance_scale: Prompt adherence (1-20)
    - seed: Random seed (optional)
    - num_images: Number of variations (1-4)
    
    **Returns:**
    - images: Generated variations as base64 PNG
    - metadata: Generation parameters
    """
    if not request.prompt.strip():
        raise HTTPException(400, "Prompt cannot be empty")
    
    try:
        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(
            _executor,
            generator.generate_variation,
            request
        )
        return result
    except Exception as e:
        logger.error(f"Variation generation failed: {e}")
        raise HTTPException(500, f"Variation failed: {str(e)}")


@router.post("/upscale", response_model=ImageGenResponse)
async def upscale_image(
    request: ImageUpscaleRequest,
    generator: TextToImageGenerator = Depends(get_generator_dep)
):
    """
    Upscale an image using AI-powered upsampling.
    
    Supports 2x and 4x upscaling with optional prompt-guided
    enhancement for better detail preservation.
    
    **Parameters:**
    - image: Base64-encoded image to upscale
    - scale: Upscaling factor (2 or 4)
    - prompt: Optional guidance for detail enhancement
    
    **Returns:**
    - images: Upscaled image as base64 PNG
    - metadata: Scale factor and timing
    """
    if request.scale not in (2, 4):
        raise HTTPException(400, "Scale must be 2 or 4")

    try:
        loop = asyncio.get_event_loop()
        result = await loop.run_in_executor(
            _executor,
            generator.upscale,
            request
        )
        return result
    except Exception as e:
        logger.error(f"Upscaling failed: {e}")
        raise HTTPException(500, f"Upscaling failed: {str(e)}")


@router.get("/models", response_model=AvailableModelsResponse)
async def list_models(
    generator: TextToImageGenerator = Depends(get_generator_dep)
):
    """
    Get information about available image generation models.
    
    Returns model capabilities, requirements, and recommended settings.
    """
    return generator.get_available_models()


@router.post("/cache/clear")
async def clear_cache(
    generator: TextToImageGenerator = Depends(get_generator_dep)
):
    """Clear the model cache to free GPU memory."""
    generator.clear_cache()
    return {"status": "ok", "message": "Cache cleared successfully"}


@router.get("/health")
async def health_check(
    generator: TextToImageGenerator = Depends(get_generator_dep)
):
    """Check the status of the image generation service."""
    import torch
    
    return {
        "status": "ok",
        "device": generator.device,
        "cuda_available": torch.cuda.is_available(),
        "cuda_device_count": torch.cuda.device_count() if torch.cuda.is_available() else 0,
        "loaded_pipelines": list(generator._pipelines.keys()),
    }
