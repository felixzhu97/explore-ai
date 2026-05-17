"""Configuration module - backward compatibility redirect.

This module is deprecated. Import from core.config instead.

    from core.config import Settings, get_settings

For new code, use:
    from core.config import Settings, VideoProvider, get_settings
"""

from .config.settings import Settings, VideoProvider, get_settings
from .image_gen_config import ImageGenSettings, get_image_gen_settings

__all__ = [
    "Settings",
    "VideoProvider",
    "get_settings",
    "ImageGenSettings",
    "get_image_gen_settings",
]
