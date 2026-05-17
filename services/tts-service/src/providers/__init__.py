"""TTS Provider module."""

from .base import BaseTTSProvider, get_provider
from .azure_tts import AzureTTSProvider
from .google_tts import GoogleTTSProvider
from .elevenlabs_tts import ElevenLabsProvider
from .coqui_tts import CoquiTTSProvider
from .edge_tts import EdgeTTSProvider

__all__ = [
    "BaseTTSProvider",
    "get_provider",
    "AzureTTSProvider",
    "GoogleTTSProvider",
    "ElevenLabsProvider",
    "CoquiTTSProvider",
    "EdgeTTSProvider",
]
