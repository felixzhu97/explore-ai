"""Image generation configuration module.

This module is kept for backward compatibility.
Use core.config instead for new code.
"""

from .config.settings import Settings, VideoProvider, get_settings

__all__ = [
    "Settings",
    "VideoProvider", 
    "get_settings",
]
