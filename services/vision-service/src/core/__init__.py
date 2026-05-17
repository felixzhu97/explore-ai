"""Core utilities and dependency injection for Vision Service.

This module provides configuration management and dependency injection
for the Vision Service application.

New code should import from the dedicated submodules:
    from core.config import Settings, get_settings
    from core.di import get_yolo, get_video_provider
"""

from .config import Settings, VideoProvider, get_settings
from .di import (
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
    # Config
    "Settings",
    "VideoProvider",
    "get_settings",
    # DI
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
