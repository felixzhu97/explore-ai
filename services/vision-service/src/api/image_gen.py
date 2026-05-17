"""Image generation API endpoints.

This module provides FastAPI routes for image generation operations.
All endpoints delegate to application layer use cases.
"""

from fastapi import APIRouter, HTTPException, Depends
import logging
import torch

from ..application.dtos.image_gen_dtos import (
    GenerateImageInputDTO,
    GenerateImageOutputDTO,
    GenerateVariationInputDTO,
    GenerateVariationOutputDTO,
    UpscaleImageInputDTO,
    UpscaleImageOutputDTO,
    ListModelsOutputDTO,
)
from ..application.use_cases.generate_image import (
    GenerateImageUseCase,
    GenerateVariationUseCase,
    UpscaleImageUseCase,
    ListModelsUseCase,
)
from ..core.di import (
    get_generate_image_use_case,
    get_generate_variation_use_case,
    get_upscale_image_use_case,
    get_list_models_use_case,
)

router = APIRouter(prefix="/image-gen", tags=["image-generation"])
logger = logging.getLogger(__name__)


@router.post("/generate", response_model=GenerateImageOutputDTO)
async def generate_image(
    request: GenerateImageInputDTO,
    use_case: GenerateImageUseCase = Depends(get_generate_image_use_case),
):
    """Generate images from text prompt.

    Uses Stable Diffusion model for image generation.
    """
    try:
        result = await use_case.execute(request)
        return result
    except ValueError as e:
        raise HTTPException(400, str(e))
    except Exception as e:
        logger.error(f"Image generation failed: {e}")
        raise HTTPException(500, f"Generation failed: {str(e)}")


@router.post("/variation", response_model=GenerateVariationOutputDTO)
async def generate_variation(
    request: GenerateVariationInputDTO,
    use_case: GenerateVariationUseCase = Depends(get_generate_variation_use_case),
):
    """Generate variations of an existing image."""
    try:
        result = await use_case.execute(request)
        return result
    except ValueError as e:
        raise HTTPException(400, str(e))
    except Exception as e:
        logger.error(f"Variation generation failed: {e}")
        raise HTTPException(500, f"Variation failed: {str(e)}")


@router.post("/upscale", response_model=UpscaleImageOutputDTO)
async def upscale_image(
    request: UpscaleImageInputDTO,
    use_case: UpscaleImageUseCase = Depends(get_upscale_image_use_case),
):
    """Upscale an image using AI-powered upsampling."""
    try:
        result = await use_case.execute(request)
        return result
    except ValueError as e:
        raise HTTPException(400, str(e))
    except Exception as e:
        logger.error(f"Upscaling failed: {e}")
        raise HTTPException(500, f"Upscaling failed: {str(e)}")


@router.get("/models", response_model=ListModelsOutputDTO)
async def list_models(
    use_case: ListModelsUseCase = Depends(get_list_models_use_case),
):
    """Get information about available image generation models."""
    try:
        return use_case.execute()
    except Exception as e:
        logger.error(f"Failed to list models: {e}")
        raise HTTPException(500, f"Failed to list models: {str(e)}")


@router.post("/cache/clear")
async def clear_cache(
    service=Depends(get_generate_image_use_case),
):
    """Clear the model cache to free GPU memory."""
    service._service.clear_cache()
    return {"status": "ok", "message": "Cache cleared successfully"}


@router.get("/health")
async def health_check(
    service=Depends(get_generate_image_use_case),
):
    """Check the status of the image generation service."""
    return {
        "status": "ok",
        "device": service._service.device,
        "cuda_available": torch.cuda.is_available(),
        "cuda_device_count": torch.cuda.device_count() if torch.cuda.is_available() else 0,
    }
