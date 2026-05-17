from .base import BaseVideoProvider
from .mock import MockVideoProvider
from .replicate import ReplicateVideoProvider
from .kling import KlingVideoProvider
from .runway import RunwayVideoProvider
from .pika import PikaVideoProvider
from .sora import SoraVideoProvider
from ..core.video_config import get_settings, VideoProvider

__all__ = [
    "BaseVideoProvider",
    "MockVideoProvider",
    "ReplicateVideoProvider",
    "KlingVideoProvider",
    "RunwayVideoProvider",
    "PikaVideoProvider",
    "SoraVideoProvider",
    "get_provider",
]


def get_provider(provider_name: str = None) -> BaseVideoProvider:
    settings = get_settings()
    
    # Use specified provider or fall back to settings
    if provider_name is None:
        provider_name = settings.VIDEO_PROVIDER

    providers = {
        VideoProvider.MOCK: MockVideoProvider,
        VideoProvider.REPLICATE: ReplicateVideoProvider,
        VideoProvider.KLING: KlingVideoProvider,
        "runway": RunwayVideoProvider,
        "pika": PikaVideoProvider,
        "sora": SoraVideoProvider,
    }

    provider_class = providers.get(provider_name, MockVideoProvider)
    return provider_class()
