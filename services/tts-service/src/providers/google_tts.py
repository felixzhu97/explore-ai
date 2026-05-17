"""Google Cloud Text-to-Speech Provider."""

from typing import List, Optional, AsyncIterator
import logging
import asyncio

from .base import BaseTTSProvider
from ..schemas import Voice, OutputFormat
from ..config import get_settings

logger = logging.getLogger(__name__)


class GoogleTTSProvider(BaseTTSProvider):
    """Google Cloud Text-to-Speech implementation."""
    
    provider_name = "google"
    
    def __init__(self):
        """Initialize Google TTS provider."""
        super().__init__()
        self._client = None
    
    def _get_client(self):
        """Get or create Google TTS client."""
        if self._client is None:
            try:
                from google.cloud import texttospeech
                
                client = texttospeech.TextToSpeechClient()
                self._client = client
                logger.info("Google TTS client initialized")
                return client
            except ImportError:
                logger.warning("Google Cloud TTS SDK not installed")
                return None
            except Exception as e:
                logger.error(f"Failed to initialize Google TTS: {e}")
                return None
        
        return self._client
    
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
        """Synthesize text to speech using Google TTS."""
        from google.cloud import texttospeech
        
        client = self._get_client()
        if client is None:
            raise RuntimeError("Google TTS client not available")
        
        # Parse voice parameter
        voice_name = voice or self.settings.default_voice
        language_code = language or self.settings.default_language
        
        # Handle voice name (e.g., "en-US-Standard-A" -> language_code="en-US", voice_name="Standard-A")
        if "-" in voice_name and not voice_name.startswith("en-") and not voice_name.startswith("zh-") and not voice_name.startswith("ja-") and not voice_name.startswith("ko-"):
            parts = voice_name.rsplit("-", 1)
            if len(parts) == 2:
                potential_lang = parts[0]
                if len(potential_lang) >= 4:  # e.g., "en-US"
                    language_code = potential_lang
                    voice_name = parts[1]
        
        # Build synthesis input
        synthesis_input = texttospeech.SynthesisInput(text=text)
        
        # Build voice configuration
        voice_params = texttospeech.VoiceSelectionParams(
            language_code=language_code,
            name=voice_name,
            ssml_gender=texttospeech.SsmlVoiceGender.FEMALE
        )
        
        # Build audio configuration
        audio_config = texttospeech.AudioConfig(
            audio_encoding=self._get_audio_encoding(output_format),
            speaking_rate=speed,
            pitch=pitch,
            sample_rate_hertz=kwargs.get('sample_rate', 24000)
        )
        
        # Synthesize
        response = client.synthesize_speech(
            input=synthesis_input,
            voice=voice_params,
            audio_config=audio_config
        )
        
        logger.debug(f"Synthesized {len(response.audio_content)} bytes of audio")
        return response.audio_content
    
    async def stream(
        self,
        text: str,
        voice: Optional[str] = None,
        language: Optional[str] = None,
        speed: float = 1.0,
        output_format: OutputFormat = OutputFormat.MP3,
        **kwargs
    ) -> AsyncIterator[bytes]:
        """Stream synthesized speech using Google TTS."""
        # For streaming, we synthesize the full audio and yield in chunks
        audio_data = self.synthesize(
            text=text,
            voice=voice,
            language=language,
            speed=speed,
            pitch=kwargs.get('pitch', 0),
            output_format=output_format,
            **kwargs
        )
        
        # Yield in chunks
        chunk_size = kwargs.get('chunk_size', 8192)
        for i in range(0, len(audio_data), chunk_size):
            yield audio_data[i:i + chunk_size]
            await asyncio.sleep(0)  # Allow other coroutines to run
    
    def _get_audio_encoding(self, output_format: OutputFormat) -> int:
        """Map output format to Google audio encoding."""
        from google.cloud import texttospeech
        
        encoding_map = {
            OutputFormat.MP3: texttospeech.AudioEncoding.MP3,
            OutputFormat.WAV: texttospeech.AudioEncoding.LINEAR16,
            OutputFormat.OGG: texttospeech.AudioEncoding.OGG_OPUS,
            OutputFormat.FLAC: texttospeech.AudioEncoding.FLAC,
        }
        return encoding_map.get(output_format, texttospeech.AudioEncoding.MP3)
    
    def list_voices(self, language: Optional[str] = None) -> List[Voice]:
        """List available Google voices."""
        try:
            from google.cloud import texttospeech
            
            client = self._get_client()
            if client is None:
                return self._get_default_voices(language)
            
            # Get voices from API
            response = client.list_voices(language_code=language)
            
            voices = []
            for voice in response.voices:
                lang_code = voice.language_codes[0] if voice.language_codes else "en-US"
                gender_map = {
                    texttospeech.SsmlVoiceGender.FEMALE: "Female",
                    texttospeech.SsmlVoiceGender.MALE: "Male",
                    texttospeech.SsmlVoiceGender.SSML_VOICE_GENDER_UNSPECIFIED: None
                }
                
                voices.append(Voice(
                    id=voice.name,
                    name=voice.name.split("-")[-1] if "-" in voice.name else voice.name,
                    language=lang_code,
                    language_name=self._get_language_name(lang_code),
                    gender=gender_map.get(voice.ssml_gender),
                    provider="google",
                    is_default=voice.name == "en-US-Standard-A"
                ))
            
            return voices
            
        except Exception as e:
            logger.warning(f"Failed to list Google voices: {e}, using defaults")
            return self._get_default_voices(language)
    
    def _get_default_voices(self, language: Optional[str] = None) -> List[Voice]:
        """Get default Google voices."""
        voices = [
            # English (US)
            Voice(id="en-US-Standard-A", name="A", language="en-US", 
                  language_name="English (US)", gender="Female", provider="google", is_default=True),
            Voice(id="en-US-Standard-B", name="B", language="en-US", 
                  language_name="English (US)", gender="Male", provider="google"),
            Voice(id="en-US-Standard-C", name="C", language="en-US", 
                  language_name="English (US)", gender="Female", provider="google"),
            Voice(id="en-US-Standard-D", name="D", language="en-US", 
                  language_name="English (US)", gender="Male", provider="google"),
            Voice(id="en-US-Wavenet-A", name="A (WaveNet)", language="en-US", 
                  language_name="English (US)", gender="Female", provider="google"),
            Voice(id="en-US-Wavenet-C", name="C (WaveNet)", language="en-US", 
                  language_name="English (US)", gender="Female", provider="google"),
            Voice(id="en-US-Wavenet-D", name="D (WaveNet)", language="en-US", 
                  language_name="English (US)", gender="Male", provider="google"),
            
            # English (UK)
            Voice(id="en-GB-Standard-A", name="A", language="en-GB", 
                  language_name="English (UK)", gender="Female", provider="google"),
            Voice(id="en-GB-Standard-B", name="B", language="en-GB", 
                  language_name="English (UK)", gender="Male", provider="google"),
            
            # Chinese
            Voice(id="zh-CN-Standard-A", name="A", language="zh-CN", 
                  language_name="Chinese (Mandarin)", gender="Female", provider="google"),
            Voice(id="zh-CN-Standard-B", name="B", language="zh-CN", 
                  language_name="Chinese (Mandarin)", gender="Male", provider="google"),
            
            # Japanese
            Voice(id="ja-JP-Standard-A", name="A", language="ja-JP", 
                  language_name="Japanese", gender="Female", provider="google"),
            Voice(id="ja-JP-Standard-B", name="B", language="ja-JP", 
                  language_name="Japanese", gender="Male", provider="google"),
            
            # Korean
            Voice(id="ko-KR-Standard-A", name="A", language="ko-KR", 
                  language_name="Korean", gender="Female", provider="google"),
            
            # French
            Voice(id="fr-FR-Standard-A", name="A", language="fr-FR", 
                  language_name="French", gender="Female", provider="google"),
            Voice(id="fr-FR-Standard-B", name="B", language="fr-FR", 
                  language_name="French", gender="Male", provider="google"),
            
            # German
            Voice(id="de-DE-Standard-A", name="A", language="de-DE", 
                  language_name="German", gender="Female", provider="google"),
            Voice(id="de-DE-Standard-B", name="B", language="de-DE", 
                  language_name="German", gender="Male", provider="google"),
            
            # Spanish
            Voice(id="es-ES-Standard-A", name="A", language="es-ES", 
                  language_name="Spanish (Spain)", gender="Female", provider="google"),
            Voice(id="es-US-Standard-A", name="A", language="es-US", 
                  language_name="Spanish (US)", gender="Female", provider="google"),
            
            # Portuguese
            Voice(id="pt-BR-Standard-A", name="A", language="pt-BR", 
                  language_name="Portuguese (Brazil)", gender="Female", provider="google"),
            
            # Italian
            Voice(id="it-IT-Standard-A", name="A", language="it-IT", 
                  language_name="Italian", gender="Female", provider="google"),
        ]
        
        if language:
            lang_prefix = language.lower()
            voices = [v for v in voices if v.language.lower().startswith(lang_prefix)]
        
        return voices
    
    def _get_language_name(self, code: str) -> str:
        """Get human-readable language name."""
        names = {
            "en-US": "English (US)",
            "en-GB": "English (UK)",
            "zh-CN": "Chinese (Mandarin)",
            "zh-TW": "Chinese (Taiwan)",
            "ja-JP": "Japanese",
            "ko-KR": "Korean",
            "fr-FR": "French",
            "de-DE": "German",
            "es-ES": "Spanish (Spain)",
            "es-US": "Spanish (US)",
            "pt-BR": "Portuguese (Brazil)",
            "pt-PT": "Portuguese (Portugal)",
            "it-IT": "Italian",
            "ru-RU": "Russian",
            "nl-NL": "Dutch",
        }
        return names.get(code, code)
    
    def health_check(self) -> bool:
        """Check Google TTS health."""
        try:
            client = self._get_client()
            if client is None:
                return False
            
            # Try a simple list_voices call
            client.list_voices(language_code="en")
            return True
        except Exception as e:
            logger.error(f"Google TTS health check failed: {e}")
            return False
