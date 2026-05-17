"""Domain ports (interface definitions).

This module contains Protocol interfaces that define contracts between layers.
Ports are defined in the domain layer but implemented by infrastructure layer.

Architecture:
    - domain/ports/     -> Interface definitions (Protocols)
    - domain/services/ -> Domain service implementations
    - infrastructure/  -> Port implementations
"""

from .video_providers import IVideoProvider, IVideoGenerationService
from .image_providers import IImageGenerationService
from .image_analysis import IObjectDetector, IImageCaptioner, IOCRProcessor

__all__ = [
    "IVideoProvider",
    "IVideoGenerationService",
    "IImageGenerationService",
    "IObjectDetector",
    "IImageCaptioner",
    "IOCRProcessor",
]
