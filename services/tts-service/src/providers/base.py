"""TTS Provider module.

This module provides a unified interface for multiple TTS providers.
"""

from typing import List, Optional, AsyncIterator, Union
from abc import ABC, abstractmethod
import hashlib
import base64

from ..schemas import Voice, OutputFormat
from ..config import get_settings


class BaseTTSProvider(ABC):
    """Abstract base class for TTS providers."""
    
    provider_name: str = "base"
    
    def __init__(self):
        """Initialize the provider."""
        self.settings = get_settings()
    
    @abstractmethod
    def synthesize(
        self,
        text: str,
        voice: Optional[str] = None,
        language: Optional[str] = None,
        speed: float = 1.0,
        pitch: float = 0,
        output_format: OutputFormat = OutputFormat.MP3,
        **kwargs
    ) -> bytes:
        """Synthesize text to speech.
        
        Args:
            text: Text to synthesize.
            voice: Voice identifier.
            language: Language code.
            speed: Speech speed multiplier.
            pitch: Pitch adjustment in Hz.
            output_format: Audio output format.
            **kwargs: Additional provider-specific parameters.
            
        Returns:
            Audio bytes.
        """
        pass
    
    @abstractmethod
    async def stream(
        self,
        text: str,
        voice: Optional[str] = None,
        language: Optional[str] = None,
        speed: float = 1.0,
        output_format: OutputFormat = OutputFormat.MP3,
        **kwargs
    ) -> AsyncIterator[bytes]:
        """Stream synthesized speech.
        
        Args:
            text: Text to synthesize.
            voice: Voice identifier.
            language: Language code.
            speed: Speech speed multiplier.
            output_format: Audio output format.
            **kwargs: Additional provider-specific parameters.
            
        Yields:
            Audio bytes chunks.
        """
        pass
    
    @abstractmethod
    def list_voices(self, language: Optional[str] = None) -> List[Voice]:
        """List available voices.
        
        Args:
            language: Filter by language code.
            
        Returns:
            List of available voices.
        """
        pass
    
    @abstractmethod
    def health_check(self) -> bool:
        """Check provider health.
        
        Returns:
            True if provider is healthy.
        """
        pass
    
    def get_cache_key(
        self,
        text: str,
        voice: Optional[str],
        language: Optional[str],
        speed: float,
        output_format: OutputFormat
    ) -> str:
        """Generate cache key for request.
        
        Args:
            text: Input text.
            voice: Voice identifier.
            language: Language code.
            speed: Speed multiplier.
            output_format: Output format.
            
        Returns:
            Cache key string.
        """
        key_data = f"{text}:{voice}:{language}:{speed}:{output_format.value}:{self.provider_name}"
        return hashlib.sha256(key_data.encode()).hexdigest()


def get_provider(provider_name: Optional[str] = None) -> BaseTTSProvider:
    """Get TTS provider instance.
    
    Args:
        provider_name: Provider name (azure, google, elevenlabs, coqui).
                     If None, uses settings default.
    
    Returns:
        TTS provider instance.
    
    Raises:
        ValueError: If provider is not supported.
    """
    from ..config import TTSProvider
    
    if provider_name is None:
        settings = get_settings()
        provider_name = settings.tts_provider.value
    
    providers = {
        TTSProvider.AZURE.value: "AzureTTSProvider",
        TTSProvider.GOOGLE.value: "GoogleTTSProvider",
        TTSProvider.ELEVENLABS.value: "ElevenLabsProvider",
        TTSProvider.COQUI.value: "CoquiTTSProvider",
        TTSProvider.EDGE.value: "EdgeTTSProvider",
    }

    if provider_name not in providers:
        available = ", ".join(providers.keys())
        raise ValueError(
            f"Unknown provider: {provider_name}. Available: {available}"
        )

    # Lazy import to avoid circular dependencies
    from .azure_tts import AzureTTSProvider
    from .google_tts import GoogleTTSProvider
    from .elevenlabs_tts import ElevenLabsProvider
    from .coqui_tts import CoquiTTSProvider
    from .edge_tts import EdgeTTSProvider

    provider_map = {
        "AzureTTSProvider": AzureTTSProvider,
        "GoogleTTSProvider": GoogleTTSProvider,
        "ElevenLabsProvider": ElevenLabsProvider,
        "CoquiTTSProvider": CoquiTTSProvider,
        "EdgeTTSProvider": EdgeTTSProvider,
    }
    
    provider_class = provider_map[providers[provider_name]]
    return provider_class()
