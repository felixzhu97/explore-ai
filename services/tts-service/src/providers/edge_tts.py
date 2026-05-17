"""Microsoft Edge TTS Provider.

Edge TTS uses Microsoft's Azure services through edge-tts library,
providing high-quality neural voices without requiring Azure credentials.
"""

from typing import List, Optional, AsyncIterator
import logging
import asyncio
import tempfile
import os

from .base import BaseTTSProvider
from ..schemas import Voice, OutputFormat
from ..config import get_settings

logger = logging.getLogger(__name__)


# Popular Edge TTS voices with metadata
EDGE_VOICES = [
    # Chinese
    Voice(id="zh-CN-XiaoxiaoNeural", name="Xiaoxiao (晓晓)", language="zh-CN", language_name="Chinese (Simp)", gender="Female", provider="edge", is_default=True),
    Voice(id="zh-CN-YunxiNeural", name="Yunxi (云希)", language="zh-CN", language_name="Chinese (Simp)", gender="Male", provider="edge"),
    Voice(id="zh-CN-YunyangNeural", name="Yunyang (云扬)", language="zh-CN", language_name="Chinese (Simp)", gender="Male", provider="edge"),
    Voice(id="zh-CN-XiaoyiNeural", name="Xiaoyi (小艺)", language="zh-CN", language_name="Chinese (Simp)", gender="Female", provider="edge"),
    Voice(id="zh-CN-liaoning-XiaobaiNeural", name="Xiaobai (小白)", language="zh-CN-LN", language_name="Chinese (Liaoning)", gender="Female", provider="edge"),
    Voice(id="zh-TW-HsiaoYuNeural", name="HsiaoYu", language="zh-TW", language_name="Chinese (Trad)", gender="Female", provider="edge"),
    Voice(id="zh-HK-HiuGaaiNeural", name="HiuGaai", language="zh-HK", language_name="Chinese (Cantonese)", gender="Female", provider="edge"),
    # English
    Voice(id="en-US-JennyNeural", name="Jenny", language="en-US", language_name="English (US)", gender="Female", provider="edge", is_default=False),
    Voice(id="en-US-GuyNeural", name="Guy", language="en-US", language_name="English (US)", gender="Male", provider="edge"),
    Voice(id="en-US-AriaNeural", name="Aria", language="en-US", language_name="English (US)", gender="Female", provider="edge"),
    Voice(id="en-US-StefanieNeural", name="Stefanie", language="en-US", language_name="English (US)", gender="Female", provider="edge"),
    Voice(id="en-GB-SoniaNeural", name="Sonia", language="en-GB", language_name="English (UK)", gender="Female", provider="edge"),
    Voice(id="en-GB-RyanNeural", name="Ryan", language="en-GB", language_name="English (UK)", gender="Male", provider="edge"),
    Voice(id="en-AU-NatashaNeural", name="Natasha", language="en-AU", language_name="English (AU)", gender="Female", provider="edge"),
    Voice(id="en-AU-CraigNeural", name="Craig", language="en-AU", language_name="English (AU)", gender="Male", provider="edge"),
    # Japanese
    Voice(id="ja-JP-NanamiNeural", name="Nanami (七海)", language="ja-JP", language_name="Japanese", gender="Female", provider="edge"),
    Voice(id="ja-JP-KeitaNeural", name="Keita (圭太)", language="ja-JP", language_name="Japanese", gender="Male", provider="edge"),
    # Korean
    Voice(id="ko-KR-SunHiNeural", name="SunHi (선희)", language="ko-KR", language_name="Korean", gender="Female", provider="edge"),
    Voice(id="ko-KR-InJoonNeural", name="InJoon (인준)", language="ko-KR", language_name="Korean", gender="Male", provider="edge"),
    # French
    Voice(id="fr-FR-DeniseNeural", name="Denise", language="fr-FR", language_name="French", gender="Female", provider="edge"),
    Voice(id="fr-FR-HenriNeural", name="Henri", language="fr-FR", language_name="French", gender="Male", provider="edge"),
    # German
    Voice(id="de-DE-KatjaNeural", name="Katja", language="de-DE", language_name="German", gender="Female", provider="edge"),
    Voice(id="de-DE-ConradNeural", name="Conrad", language="de-DE", language_name="German", gender="Male", provider="edge"),
    # Spanish
    Voice(id="es-ES-ElviraNeural", name="Elvira", language="es-ES", language_name="Spanish", gender="Female", provider="edge"),
    Voice(id="es-MX-DaliaNeural", name="Dalia", language="es-MX", language_name="Spanish (MX)", gender="Female", provider="edge"),
    # Portuguese
    Voice(id="pt-BR-FranciscaNeural", name="Francisca", language="pt-BR", language_name="Portuguese (BR)", gender="Female", provider="edge"),
    Voice(id="pt-PT-RaquelNeural", name="Raquel", language="pt-PT", language_name="Portuguese (PT)", gender="Female", provider="edge"),
    # Italian
    Voice(id="it-IT-ElsaNeural", name="Elsa", language="it-IT", language_name="Italian", gender="Female", provider="edge"),
    Voice(id="it-IT-DiegoNeural", name="Diego", language="it-IT", language_name="Italian", gender="Male", provider="edge"),
    # Russian
    Voice(id="ru-RU-SvetlanaNeural", name="Svetlana", language="ru-RU", language_name="Russian", gender="Female", provider="edge"),
    Voice(id="ru-RU-DmitryNeural", name="Dmitry", language="ru-RU", language_name="Russian", gender="Male", provider="edge"),
    # Hindi
    Voice(id="hi-IN-SwaraNeural", name="Swara", language="hi-IN", language_name="Hindi", gender="Female", provider="edge"),
    Voice(id="hi-IN-MadhurNeural", name="Madhur", language="hi-IN", language_name="Hindi", gender="Male", provider="edge"),
]


class EdgeTTSProvider(BaseTTSProvider):
    """Microsoft Edge TTS implementation using edge-tts library."""

    provider_name = "edge"

    def __init__(self):
        """Initialize Edge TTS provider."""
        super().__init__()
        self._communicate = None
        self._edge_imported = False

    def _ensure_edge_tts(self):
        """Lazy import of edge-tts to avoid startup errors."""
        if not self._edge_imported:
            try:
                import edge_tts
                self._communicate = edge_tts.Communicate
                self._edge_imported = True
                logger.info("Edge TTS library loaded successfully")
            except ImportError:
                logger.error("edge-tts not installed. Install with: pip install edge-tts")
                raise RuntimeError("edge-tts library not installed")
        return self._communicate

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
        """Synchronous synthesis using Edge TTS.

        Args:
            text: Text to synthesize.
            voice: Voice identifier (e.g., 'zh-CN-XiaoxiaoNeural').
            language: Language code (auto-detected if not specified).
            speed: Speech rate multiplier (0.5 to 2.0, maps to edge-tts format).
            pitch: Pitch adjustment in percentage (-50% to +50%).
            output_format: Audio output format.

        Returns:
            Audio bytes.
        """
        Communicate = self._ensure_edge_tts()

        voice = voice or self.settings.default_voice
        rate = self._speed_to_edge_rate(speed)
        pitch_adj = self._pitch_to_edge_pitch(pitch)

        output_path = tempfile.mktemp(suffix=".mp3")
        try:
            asyncio.run(self._synthesize_async(Communicate, text, voice, rate, pitch_adj, output_path))
            with open(output_path, "rb") as f:
                audio_data = f.read()
            logger.debug(f"Edge TTS synthesized {len(audio_data)} bytes")
            return audio_data
        finally:
            if os.path.exists(output_path):
                os.unlink(output_path)

    async def _synthesize_async(
        self,
        Communicate,
        text: str,
        voice: str,
        rate: str,
        pitch: str,
        output_path: str
    ):
        """Async helper for synthesis."""
        communicate = Communicate(text, voice, rate=rate, pitch=pitch)
        await communicate.save(output_path)

    def _speed_to_edge_rate(self, speed: float) -> str:
        """Convert 0.5-2.0 speed to edge-tts rate format (+/-X%)."""
        if speed < 0.5:
            speed = 0.5
        elif speed > 2.0:
            speed = 2.0
        percentage = (speed - 1.0) * 100
        return f"{'+' if percentage >= 0 else ''}{percentage:.0f}%"

    def _pitch_to_edge_pitch(self, pitch: float) -> str:
        """Convert Hz pitch to edge-tts pitch format (+/-X%)."""
        return f"{'+' if pitch >= 0 else ''}{pitch:.0f}%"

    async def stream(
        self,
        text: str,
        voice: Optional[str] = None,
        language: Optional[str] = None,
        speed: float = 1.0,
        output_format: OutputFormat = OutputFormat.MP3,
        **kwargs
    ) -> AsyncIterator[bytes]:
        """Stream synthesized speech from Edge TTS."""
        Communicate = self._ensure_edge_tts()

        voice = voice or self.settings.default_voice
        rate = self._speed_to_edge_rate(speed)
        pitch = self._pitch_to_edge_pitch(kwargs.get('pitch', 0))

        communicate = Communicate(text, voice, rate=rate, pitch=pitch)
        chunk_size = kwargs.get('chunk_size', 8192)

        async for chunk in communicate.stream():
            yield chunk

    def list_voices(self, language: Optional[str] = None) -> List[Voice]:
        """List available Edge TTS voices.

        Args:
            language: Filter by language code (e.g., 'zh-CN', 'en-US').

        Returns:
            List of available voices.
        """
        if language:
            lang_prefix = language.lower()
            filtered = [v for v in EDGE_VOICES if v.language.lower().startswith(lang_prefix)]
            return filtered if filtered else EDGE_VOICES
        return EDGE_VOICES

    def health_check(self) -> bool:
        """Check Edge TTS availability."""
        try:
            self._ensure_edge_tts()
            return True
        except Exception as e:
            logger.error(f"Edge TTS health check failed: {e}")
            return False
