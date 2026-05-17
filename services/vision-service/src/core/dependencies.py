"""Dependency injection module - backward compatibility redirect.

This module is deprecated. Import from core.di instead.

    from core.di import get_yolo, get_blip

For new code, use:
    from core.di import (
        get_yolo, get_blip, get_easyocr, get_generator,
        get_video_provider, get_video_generation_service,
        ModelContainer, _reset_instances
    )

Note: Protocol types (ObjectDetector, ImageCaptioner, OCRProcessor, ImageGenerator)
have been removed from this module. Import them from domain.ports instead.
"""

from .di.container import (
    ModelContainer,
    get_yolo,
    get_blip,
    get_easyocr,
    get_generator,
    get_video_provider,
    get_video_generation_service,
    get_image_generation_service,
    get_generate_image_use_case,
    get_generate_variation_use_case,
    get_upscale_image_use_case,
    get_list_models_use_case,
    get_analyze_image_use_case,
    _reset_instances,
)

__all__ = [
    "ModelContainer",
    "get_yolo",
    "get_blip",
    "get_easyocr",
    "get_generator",
    "get_video_provider",
    "get_video_generation_service",
    "get_image_generation_service",
    "get_generate_image_use_case",
    "get_generate_variation_use_case",
    "get_upscale_image_use_case",
    "get_list_models_use_case",
    "get_analyze_image_use_case",
    "_reset_instances",
]
