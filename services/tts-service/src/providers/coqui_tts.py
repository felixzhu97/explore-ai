"""Coqui TTS (Local) Provider."""

from typing import List, Optional, AsyncIterator, Dict
import logging
import asyncio
import tempfile
import os

from .base import BaseTTSProvider
from ..schemas import Voice, OutputFormat
from ..config import get_settings

logger = logging.getLogger(__name__)


class CoquiTTSProvider(BaseTTSProvider):
    """Coqui TTS implementation for local, privacy-focused deployment."""
    
    provider_name = "coqui"
    
    def __init__(self):
        """Initialize Coqui TTS provider."""
        super().__init__()
        self._model = None
        self._model_loaded = False
    
    def _load_model(self):
        """Load Coqui TTS model."""
        if self._model_loaded:
            return self._model
        
        try:
            from TTS.api import TTS
            
            model_path = self.settings.coqui_model_path
            if model_path:
                self._model = TTS(model_path=model_path, gpu=False)
            else:
                # Use default model
                self._model = TTS(model_name="tts_models/en/ljspeech/tacotron2-DDC", gpu=False)
            
            self._model_loaded = True
            logger.info("Coqui TTS model loaded successfully")
            return self._model
            
        except ImportError:
            logger.warning("TTS library not installed. Install with: pip install TTS")
            return None
        except Exception as e:
            logger.error(f"Failed to load Coqui TTS model: {e}")
            return None
    
    def synthesize(
        self,
        text: str,
        voice: Optional[str] = None,
        language: Optional[str] = None,
        speed: float = 1.0,
        pitch: float = 0,
        output_format: OutputFormat = OutputFormat.WAV,
        **kwargs
    ) -> bytes:
        """Synthesize text to speech using Coqui TTS."""
        model = self._load_model()
        if model is None:
            raise RuntimeError("Coqui TTS model not available")
        
        # Create temporary file for output
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
            output_path = f.name
        
        try:
            # Generate speech
            speaker_id = kwargs.get("speaker_id")
            
            if hasattr(model, 'tts_with_vc') and voice:
                # Voice conversion mode
                logger.debug(f"Using voice conversion with speaker: {voice}")
                model.tts_with_vc(
                    text=text,
                    speaker_wav=voice,  # voice can be a path to reference audio
                    file_path=output_path
                )
            else:
                # Standard TTS
                model.tts(
                    text=text,
                    speaker_id=speaker_id,
                    file_path=output_path
                )
            
            # Read generated audio
            with open(output_path, "rb") as f:
                audio_data = f.read()
            
            # Convert format if needed
            if output_format != OutputFormat.WAV:
                audio_data = self._convert_format(output_path, output_format)
            
            logger.debug(f"Synthesized {len(audio_data)} bytes of audio")
            return audio_data
            
        finally:
            # Clean up temp file
            if os.path.exists(output_path):
                os.unlink(output_path)
    
    async def stream(
        self,
        text: str,
        voice: Optional[str] = None,
        language: Optional[str] = None,
        speed: float = 1.0,
        output_format: OutputFormat = OutputFormat.WAV,
        **kwargs
    ) -> AsyncIterator[bytes]:
        """Stream synthesized speech using Coqui TTS."""
        # For local TTS, we synthesize the full audio and yield in chunks
        audio_data = self.synthesize(
            text=text,
            voice=voice,
            language=language,
            speed=speed,
            pitch=pitch,
            output_format=output_format,
            **kwargs
        )
        
        # Yield in chunks
        chunk_size = kwargs.get('chunk_size', 8192)
        for i in range(0, len(audio_data), chunk_size):
            yield audio_data[i:i + chunk_size]
            await asyncio.sleep(0)  # Allow other coroutines to run
    
    def _convert_format(self, input_path: str, output_format: OutputFormat) -> bytes:
        """Convert WAV audio to another format."""
        try:
            from pydub import AudioSegment
            
            audio = AudioSegment.from_wav(input_path)
            
            if output_format == OutputFormat.MP3:
                output_path = input_path.replace(".wav", ".mp3")
                audio.export(output_path, format="mp3", bitrate="128k")
            elif output_format == OutputFormat.OGG:
                output_path = input_path.replace(".wav", ".ogg")
                audio.export(output_path, format="ogg")
            elif output_format == OutputFormat.FLAC:
                output_path = input_path.replace(".wav", ".flac")
                audio.export(output_path, format="flac")
            else:
                # Return original WAV
                with open(input_path, "rb") as f:
                    return f.read()
            
            with open(output_path, "rb") as f:
                data = f.read()
            
            # Clean up
            os.unlink(output_path)
            
            return data
            
        except ImportError:
            logger.warning("pydub not installed for format conversion")
            with open(input_path, "rb") as f:
                return f.read()
    
    def list_voices(self, language: Optional[str] = None) -> List[Voice]:
        """List available Coqui voices/models."""
        # Coqui models are model configurations rather than predefined voices
        # Return model-based voice options
        voices = [
            Voice(
                id="default",
                name="Default (LJSpeech)",
                language="en",
                language_name="English",
                gender=None,
                provider="coqui",
                is_default=True
            ),
            Voice(
                id="vctk",
                name="VCTK (Multi-speaker)",
                language="en",
                language_name="English (Multi)",
                gender=None,
                provider="coqui"
            ),
            Voice(
                id="libri_tts",
                name="LibriTTS (Multi-speaker)",
                language="en",
                language_name="English (Libri)",
                gender=None,
                provider="coqui"
            ),
            Voice(
                id="custom",
                name="Custom Speaker (Reference Audio)",
                language="multi",
                language_name="Multi-language",
                gender=None,
                provider="coqui"
            ),
        ]
        
        if language:
            voices = [v for v in voices if language.lower() in v.language.lower()]
        
        return voices
    
    def get_available_models(self) -> List[Dict]:
        """Get list of available Coqui TTS models."""
        try:
            from TTS.api import TTS
            
            # List all available models (requires internet for first run)
            models = TTS.list_models()
            return models
        except ImportError:
            logger.warning("TTS library not installed")
            return []
        except Exception as e:
            logger.error(f"Failed to list Coqui models: {e}")
            return []
    
    def health_check(self) -> bool:
        """Check Coqui TTS health."""
        try:
            model = self._load_model()
            return model is not None
        except Exception as e:
            logger.error(f"Coqui TTS health check failed: {e}")
            return False
