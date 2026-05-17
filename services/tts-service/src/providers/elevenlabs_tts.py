"""ElevenLabs TTS Provider."""

from typing import List, Optional, AsyncIterator, Dict, Any
import logging
import asyncio
import base64
import json

from .base import BaseTTSProvider
from ..schemas import Voice, OutputFormat
from ..config import get_settings

logger = logging.getLogger(__name__)


class ElevenLabsProvider(BaseTTSProvider):
    """ElevenLabs TTS implementation."""
    
    provider_name = "elevenlabs"
    
    # Voice settings presets
    VOICE_SETTINGS = {
        "stability": 0.5,
        "similarity_boost": 0.75,
        "style": 0.0,
        "use_speaker_boost": True
    }
    
    def __init__(self):
        """Initialize ElevenLabs TTS provider."""
        super().__init__()
        self._api_key = self.settings.elevenlabs_api_key
        self._voices_cache: Optional[List[Voice]] = None
    
    def _get_headers(self) -> Dict[str, str]:
        """Get API headers."""
        return {
            "xi-api-key": self._api_key,
            "Content-Type": "application/json"
        }
    
    def _get_base_url(self) -> str:
        """Get API base URL."""
        return "https://api.elevenlabs.io/v1"
    
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
        """Synthesize text to speech using ElevenLabs."""
        import httpx
        
        voice_id = voice or self.settings.default_voice
        
        # Map output format to ElevenLabs format
        format_map = {
            OutputFormat.MP3: "mp3_44100_128",
            OutputFormat.WAV: "wav_44100",
            OutputFormat.OGG: "ogg_v1",
        }
        output_format_str = format_map.get(output_format, "mp3_44100_128")
        
        # Build request
        url = f"{self._get_base_url()}/text-to-speech/{voice_id}"
        
        # Convert speed from multiplier to percentage (ElevenLabs uses 0.5-2.0)
        speed_value = min(max(speed, 0.5), 2.0)
        
        payload = {
            "text": text,
            "model_id": kwargs.get("model_id", "eleven_multilingual_v2"),
            "voice_settings": {
                "stability": kwargs.get("stability", 0.5),
                "similarity_boost": kwargs.get("similarity_boost", 0.75),
                "style": kwargs.get("style", 0.0),
                "use_speaker_boost": kwargs.get("use_speaker_boost", True),
            }
        }
        
        # Add language hint if specified
        if language:
            payload["language_code"] = language
        
        try:
            with httpx.Client(timeout=60.0) as client:
                response = client.post(
                    url,
                    json=payload,
                    headers=self._get_headers()
                )
                
                if response.status_code == 200:
                    logger.debug(f"Synthesized {len(response.content)} bytes of audio")
                    return response.content
                else:
                    error_detail = response.json().get("detail", response.text)
                    raise RuntimeError(f"ElevenLabs API error: {error_detail}")
                    
        except ImportError:
            raise RuntimeError("httpx is required for ElevenLabs TTS")
    
    async def stream(
        self,
        text: str,
        voice: Optional[str] = None,
        language: Optional[str] = None,
        speed: float = 1.0,
        output_format: OutputFormat = OutputFormat.MP3,
        **kwargs
    ) -> AsyncIterator[bytes]:
        """Stream synthesized speech using ElevenLabs."""
        import httpx
        
        voice_id = voice or self.settings.default_voice
        
        # Map output format
        format_map = {
            OutputFormat.MP3: "mp3_44100_128",
            OutputFormat.WAV: "wav_44100",
            OutputFormat.OGG: "ogg_v1",
        }
        output_format_str = format_map.get(output_format, "mp3_44100_128")
        
        # Convert speed
        speed_value = min(max(speed, 0.5), 2.0)
        
        payload = {
            "text": text,
            "model_id": kwargs.get("model_id", "eleven_multilingual_v2"),
            "voice_settings": {
                "stability": kwargs.get("stability", 0.5),
                "similarity_boost": kwargs.get("similarity_boost", 0.75),
                "style": kwargs.get("style", 0.0),
                "use_speaker_boost": kwargs.get("use_speaker_boost", True),
            }
        }
        
        if language:
            payload["language_code"] = language
        
        url = f"{self._get_base_url()}/text-to-speech/{voice_id}/stream"
        
        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                async with client.stream(
                    "POST",
                    url,
                    json=payload,
                    headers=self._get_headers()
                ) as response:
                    if response.status_code != 200:
                        error_detail = response.json().get("detail", response.text)
                        raise RuntimeError(f"ElevenLabs API error: {error_detail}")
                    
                    async for chunk in response.aiter_bytes(chunk_size=8192):
                        if chunk:
                            yield chunk
                            
        except ImportError:
            raise RuntimeError("httpx is required for ElevenLabs TTS")
    
    def list_voices(self, language: Optional[str] = None) -> List[Voice]:
        """List available ElevenLabs voices."""
        if self._voices_cache is not None:
            voices = self._voices_cache
        else:
            try:
                import httpx
                
                with httpx.Client(timeout=30.0) as client:
                    response = client.get(
                        f"{self._get_base_url()}/voices",
                        headers={
                            "xi-api-key": self._api_key
                        }
                    )
                    
                    if response.status_code == 200:
                        data = response.json()
                        voices = []
                        
                        for voice_data in data.get("voices", []):
                            voices.append(Voice(
                                id=voice_data.get("voice_id", ""),
                                name=voice_data.get("name", ""),
                                language=voice_data.get("language", "en"),
                                language_name=voice_data.get("language_name", voice_data.get("language", "en")),
                                gender=voice_data.get("labels", {}).get("gender"),
                                provider="elevenlabs",
                                is_default=False
                            ))
                        
                        self._voices_cache = voices
                    else:
                        voices = self._get_default_voices()
                        
            except Exception as e:
                logger.warning(f"Failed to fetch ElevenLabs voices: {e}")
                voices = self._get_default_voices()
        
        if language:
            lang_prefix = language.lower()
            voices = [v for v in voices if v.language.lower().startswith(lang_prefix)]
        
        return voices
    
    def _get_default_voices(self) -> List[Voice]:
        """Get default ElevenLabs voices."""
        return [
            Voice(
                id="EXAVITQu4vr4xnSDxMaL", 
                name="Bella", 
                language="en",
                language_name="English",
                gender="Female",
                provider="elevenlabs",
                is_default=True
            ),
            Voice(
                id="VR6AewLTigWG4xSOukaG", 
                name="Arnold", 
                language="en",
                language_name="English",
                gender="Male",
                provider="elevenlabs"
            ),
            Voice(
                id="pFZP5JQG7iQjIQuC4Bku", 
                name="Charlie", 
                language="en",
                language_name="English",
                gender="Male",
                provider="elevenlabs"
            ),
            Voice(
                id="TX3LPaxmHKxFdv7VOQHJ", 
                name="Lily", 
                language="zh",
                language_name="Chinese",
                gender="Female",
                provider="elevenlabs"
            ),
            Voice(
                id="gD5ZtGQKNwLqmgNQhQO", 
                name="Kore", 
                language="ja",
                language_name="Japanese",
                gender="Male",
                provider="elevenlabs"
            ),
            Voice(
                id="lbOMEXYFXZhcEwiNMCDY", 
                name="Alice", 
                language="ko",
                language_name="Korean",
                gender="Female",
                provider="elevenlabs"
            ),
            Voice(
                id="CGJQgCGDZLRPDYfiygVY", 
                name="Marie", 
                language="fr",
                language_name="French",
                gender="Female",
                provider="elevenlabs"
            ),
            Voice(
                id="XrExE9yKIg1WjnnlVkGX", 
                name="Kasper", 
                language="de",
                language_name="German",
                gender="Male",
                provider="elevenlabs"
            ),
            Voice(
                id="pMsXgVXvWNBLbNvUWzxe", 
                name="Sofia", 
                language="es",
                language_name="Spanish",
                gender="Female",
                provider="elevenlabs"
            ),
            Voice(
                id="zcAOhNBS3c98A8zXpJGr", 
                name="Giorgio", 
                language="it",
                language_name="Italian",
                gender="Male",
                provider="elevenlabs"
            ),
        ]
    
    def get_voice_settings(self, voice_id: str) -> Dict[str, Any]:
        """Get settings for a specific voice."""
        import httpx
        
        try:
            with httpx.Client(timeout=30.0) as client:
                response = client.get(
                    f"{self._get_base_url()}/voices/{voice_id}/settings",
                    headers={"xi-api-key": self._api_key}
                )
                
                if response.status_code == 200:
                    return response.json()
                    
        except Exception as e:
            logger.warning(f"Failed to get voice settings: {e}")
        
        return self.VOICE_SETTINGS.copy()
    
    def health_check(self) -> bool:
        """Check ElevenLabs API health."""
        try:
            import httpx
            
            with httpx.Client(timeout=10.0) as client:
                response = client.get(
                    f"{self._get_base_url()}/user",
                    headers={"xi-api-key": self._api_key}
                )
                
                return response.status_code == 200
                
        except Exception as e:
            logger.error(f"ElevenLabs health check failed: {e}")
            return False
