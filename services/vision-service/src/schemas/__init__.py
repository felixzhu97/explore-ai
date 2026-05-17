from .vision import (
    TaskType,
    DetectionResult,
    DetectionResponse,
    CaptionResponse,
    OCRResult,
    OCRResponse,
)
from .image_gen import (
    ImageModel,
    AspectRatio,
    ASPECT_RATIO_DIMENSIONS,
    ImageGenRequest,
    ImageGenResponse,
    ImageVariationRequest,
    ImageUpscaleRequest,
    ModelInfo,
    AvailableModelsResponse,
)

__all__ = [
    # Vision schemas
    "TaskType",
    "DetectionResult",
    "DetectionResponse",
    "CaptionResponse",
    "OCRResult",
    "OCRResponse",
    # Image generation schemas
    "ImageModel",
    "AspectRatio",
    "ASPECT_RATIO_DIMENSIONS",
    "ImageGenRequest",
    "ImageGenResponse",
    "ImageVariationRequest",
    "ImageUpscaleRequest",
    "ModelInfo",
    "AvailableModelsResponse",
]
