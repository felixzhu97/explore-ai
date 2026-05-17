"""Video provider factory and exports.

This module provides a factory function for creating video provider instances
with dependency injection support.

Providers are infrastructure concerns that handle external API integrations.
"""

from .base import BaseVideoProvider
from .mock import MockVideoProvider
from .replicate import ReplicateVideoProvider
from .kling import KlingVideoProvider
from .runway import RunwayVideoProvider
from .pika import PikaVideoProvider
from .sora import SoraVideoProvider

# Use VideoProvider enum from core config for factory mapping
# Note: core.config is allowed here since config is at the same layer as infrastructure
from ...core.config.settings import VideoProvider as VideoProviderEnum

# Use VideoProvider Protocol from domain layer for type hints
from ...domain.ports.video_providers import IVideoProvider

__all__ = [
    "BaseVideoProvider",
    "MockVideoProvider",
    "ReplicateVideoProvider",
    "KlingVideoProvider",
    "RunwayVideoProvider",
    "PikaVideoProvider",
    "SoraVideoProvider",
    "get_provider",
    "IVideoProvider",
]


def get_provider(
    provider_name: str = None,
    api_key: str = "",
    api_secret: str = "",
) -> IVideoProvider:
    """Factory function to create video providers.
    
    Supports dependency injection by accepting API credentials as parameters,
    enabling easy mocking in unit tests.
    
    Args:
        provider_name: Provider name (defaults to settings.VIDEO_PROVIDER).
        api_key: API key for the provider.
        api_secret: API secret (for Kling).
        
    Returns:
        Configured provider instance.
    """
    from ...core.config.settings import get_settings
    
    if provider_name is None:
        settings = get_settings()
        provider_name = settings.VIDEO_PROVIDER

    provider_map = {
        VideoProviderEnum.MOCK: MockVideoProvider,
        VideoProviderEnum.REPLICATE: ReplicateVideoProvider,
        VideoProviderEnum.KLING: KlingVideoProvider,
        VideoProviderEnum.RUNWAY: RunwayVideoProvider,
        VideoProviderEnum.PIKA: PikaVideoProvider,
        VideoProviderEnum.SORA: SoraVideoProvider,
    }
    
    provider_class = provider_map.get(provider_name, MockVideoProvider)
    
    # Pass appropriate credentials based on provider type
    if provider_name == VideoProviderEnum.KLING:
        return provider_class(api_key=api_key, api_secret=api_secret)
    elif provider_name == VideoProviderEnum.REPLICATE:
        return provider_class(api_token=api_key)
    elif provider_name in (VideoProviderEnum.PIKA, VideoProviderEnum.RUNWAY, VideoProviderEnum.SORA):
        return provider_class(api_key=api_key)
    else:
        return provider_class()
