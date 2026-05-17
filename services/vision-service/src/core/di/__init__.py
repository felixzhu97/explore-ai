"""Dependency injection module for Vision Service."""

from .container import (
    ModelContainer,
    get_yolo,
    get_blip,
    get_easyocr,
    get_generator,
    get_video_provider,
    get_video_generation_service,
    get_image_generation_service,
    get_image_generation_rules,
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
    "get_image_generation_rules",
    "get_generate_image_use_case",
    "get_generate_variation_use_case",
    "get_upscale_image_use_case",
    "get_list_models_use_case",
    "get_analyze_image_use_case",
    "_reset_instances",
]
