"""Azure Cognitive Services TTS Provider."""

from typing import List, Optional, AsyncIterator
import logging

from .base import BaseTTSProvider
from ..schemas import Voice, OutputFormat
from ..config import get_settings

logger = logging.getLogger(__name__)


class AzureTTSProvider(BaseTTSProvider):
    """Azure Cognitive Services TTS implementation."""
    
    provider_name = "azure"
    
    def __init__(self):
        """Initialize Azure TTS provider."""
        super().__init__()
        self._client = None
        self._voices_cache: Optional[List[Voice]] = None
    
    def _get_client(self):
        """Get or create Azure TTS client."""
        if self._client is None:
            try:
                import azure.cognitiveservices.speech as speechsdk
                
                speech_config = speechsdk.SpeechConfig(
                    subscription=self.settings.azure_speech_key,
                    region=self.settings.azure_speech_region
                )
                
                self._client = speech_config
                logger.info("Azure TTS client initialized")
            except ImportError:
                logger.warning("Azure SDK not installed")
                return None
            except Exception as e:
                logger.error(f"Failed to initialize Azure TTS: {e}")
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
        """Synthesize text to speech using Azure TTS."""
        import azure.cognitiveservices.speech as speechsdk
        from azure.cognitiveservices.speech import SpeechSynthesisOutputFormat
        import threading
        import time
        
        speech_config = self._get_client()
        if speech_config is None:
            raise RuntimeError("Azure TTS client not available")
        
        # Set voice
        voice = voice or self.settings.default_voice
        speech_config.speech_synthesis_voice_name = voice
        
        # Set output format
        format_map = {
            OutputFormat.MP3: SpeechSynthesisOutputFormat.Audio16Khz32KBitRateMonoMp3,
            OutputFormat.WAV: SpeechSynthesisOutputFormat.Audio16Khz16BitMonoPcm,
            OutputFormat.OGG: SpeechSynthesisOutputFormat.Ogg16Khz16BitMonoOpus,
        }
        speech_config.set_speech_synthesis_output_format(
            format_map.get(output_format, SpeechSynthesisOutputFormat.Audio16Khz32KBitRateMonoMp3)
        )
        
        # Create synthesizer
        synthesizer = speechsdk.SpeechSynthesizer(speech_config=speech_config)
        
        # SSML for speed and pitch
        pitch_str = f"+{pitch}Hz" if pitch >= 0 else f"{pitch}Hz"
        speed_str = str(int(speed * 100))
        
        ssml = f"""<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='{language or self.settings.default_language}'>
            <voice name='{voice}'>
                <prosody rate='{speed_str}%' pitch='{pitch_str}'>{text}</prosody>
            </voice>
        </speak>"""
        
        # Synthesize
        result = synthesizer.speak_ssml_async(ssml).get()
        
        if result.reason == speechsdk.ResultReason.SynthesizingAudioCompleted:
            audio_data = result.audio_data
            logger.debug(f"Synthesized {len(audio_data)} bytes of audio")
            return bytes(audio_data)
        else:
            error_details = result.cancellation_details.error_details if hasattr(result, 'cancellation_details') else "Unknown"
            raise RuntimeError(f"TTS synthesis failed: {error_details}")
    
    async def stream(
        self,
        text: str,
        voice: Optional[str] = None,
        language: Optional[str] = None,
        speed: float = 1.0,
        output_format: OutputFormat = OutputFormat.MP3,
        **kwargs
    ) -> AsyncIterator[bytes]:
        """Stream synthesized speech using Azure TTS."""
        import azure.cognitiveservices.speech as speechsdk
        from azure.cognitiveservices.speech import SpeechSynthesisOutputFormat
        import asyncio
        
        speech_config = self._get_client()
        if speech_config is None:
            raise RuntimeError("Azure TTS client not available")
        
        # Set voice
        voice = voice or self.settings.default_voice
        speech_config.speech_synthesis_voice_name = voice
        
        # Set output format
        format_map = {
            OutputFormat.MP3: SpeechSynthesisOutputFormat.Audio16Khz32KBitRateMonoMp3,
            OutputFormat.WAV: SpeechSynthesisOutputFormat.Audio16Khz16BitMonoPcm,
            OutputFormat.OGG: SpeechSynthesisOutputFormat.Ogg16Khz16BitMonoOpus,
        }
        speech_config.set_speech_synthesis_output_format(
            format_map.get(output_format, SpeechSynthesisOutputFormat.Audio16Khz32KBitRateMonoMp3)
        )
        
        # Create synthesizer
        synthesizer = speechsdk.SpeechSynthesizer(speech_config=speech_config)
        
        # SSML for speed and pitch
        pitch = kwargs.get('pitch', 0)
        pitch_str = f"+{pitch}Hz" if pitch >= 0 else f"{pitch}Hz"
        speed_str = str(int(speed * 100))
        
        ssml = f"""<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='{language or self.settings.default_language}'>
            <voice name='{voice}'>
                <prosody rate='{speed_str}%' pitch='{pitch_str}'>{text}</prosody>
            </voice>
        </speak>"""
        
        # Event for streaming
        audio_chunks = []
        done_event = asyncio.Event()
        error_ref = [None]
        
        def handle(evt):
            if hasattr(evt, 'result') and hasattr(evt.result, 'audio_data'):
                audio_chunks.append(bytes(evt.result.audio_data))
        
        def handle_error(evt):
            error_ref[0] = evt.error_details
            done_event.set()
        
        def handle_complete(evt):
            done_event.set()
        
        # Set up callbacks
        synthesizer.synthesizing.connect(handle)
        synthesizer.synthesis_canceled.connect(handle_error)
        synthesizer.synthesis_completed.connect(handle_complete)
        
        # Start synthesis
        synthesizer.start_speaking_ssml_async(ssml)
        
        # Wait and yield chunks
        while not done_event.is_set():
            if audio_chunks:
                chunk = audio_chunks.pop(0)
                yield chunk
            else:
                await asyncio.sleep(0.05)
        
        # Yield remaining chunks
        while audio_chunks:
            yield audio_chunks.pop(0)
        
        # Check for errors
        if error_ref[0]:
            raise RuntimeError(f"Streaming error: {error_ref[0]}")
    
    def list_voices(self, language: Optional[str] = None) -> List[Voice]:
        """List available Azure voices."""
        if self._voices_cache is not None:
            voices = self._voices_cache
        else:
            # Common Azure Neural voices
            voices = [
                # English voices
                Voice(id="en-US-JennyNeural", name="Jenny", language="en-US", 
                      language_name="English (US)", gender="Female", provider="azure", is_default=True),
                Voice(id="en-US-GuyNeural", name="Guy", language="en-US", 
                      language_name="English (US)", gender="Male", provider="azure"),
                Voice(id="en-US-AriaNeural", name="Aria", language="en-US", 
                      language_name="English (US)", gender="Female", provider="azure"),
                Voice(id="en-US-StefanNeural", name="Stefan", language="en-US", 
                      language_name="English (US)", gender="Male", provider="azure"),
                Voice(id="en-GB-SoniaNeural", name="Sonia", language="en-GB", 
                      language_name="English (UK)", gender="Female", provider="azure"),
                Voice(id="en-GB-RyanNeural", name="Ryan", language="en-GB", 
                      language_name="English (UK)", gender="Male", provider="azure"),
                Voice(id="en-AU-NatashaNeural", name="Natasha", language="en-AU", 
                      language_name="English (Australia)", gender="Female", provider="azure"),
                Voice(id="en-AU-CraigNeural", name="Craig", language="en-AU", 
                      language_name="English (Australia)", gender="Male", provider="azure"),
                
                # Chinese voices
                Voice(id="zh-CN-XiaoxiaoNeural", name="Xiaoxiao", language="zh-CN", 
                      language_name="Chinese (Mandarin, Simplified)", gender="Female", provider="azure"),
                Voice(id="zh-CN-YunxiNeural", name="Yunxi", language="zh-CN", 
                      language_name="Chinese (Mandarin, Simplified)", gender="Male", provider="azure"),
                Voice(id="zh-TW-HsiaoChenNeural", name="HsiaoChen", language="zh-TW", 
                      language_name="Chinese (Taiwan)", gender="Female", provider="azure"),
                
                # Japanese voices
                Voice(id="ja-JP-NanamiNeural", name="Nanami", language="ja-JP", 
                      language_name="Japanese", gender="Female", provider="azure"),
                Voice(id="ja-JP-KeitaNeural", name="Keita", language="ja-JP", 
                      language_name="Japanese", gender="Male", provider="azure"),
                
                # Korean voices
                Voice(id="ko-KR-SunHiNeural", name="SunHi", language="ko-KR", 
                      language_name="Korean", gender="Female", provider="azure"),
                Voice(id="ko-KR-InJoonNeural", name="InJoon", language="ko-KR", 
                      language_name="Korean", gender="Male", provider="azure"),
                
                # French voices
                Voice(id="fr-FR-DeniseNeural", name="Denise", language="fr-FR", 
                      language_name="French (France)", gender="Female", provider="azure"),
                Voice(id="fr-FR-HenriNeural", name="Henri", language="fr-FR", 
                      language_name="French (France)", gender="Male", provider="azure"),
                
                # German voices
                Voice(id="de-DE-KatjaNeural", name="Katja", language="de-DE", 
                      language_name="German (Germany)", gender="Female", provider="azure"),
                Voice(id="de-DE-ConradNeural", name="Conrad", language="de-DE", 
                      language_name="German (Germany)", gender="Male", provider="azure"),
                
                # Spanish voices
                Voice(id="es-ES-ElviraNeural", name="Elvira", language="es-ES", 
                      language_name="Spanish (Spain)", gender="Female", provider="azure"),
                Voice(id="es-MX-DaliaNeural", name="Dalia", language="es-MX", 
                      language_name="Spanish (Mexico)", gender="Female", provider="azure"),
                
                # Portuguese voices
                Voice(id="pt-BR-FranciscaNeural", name="Francisca", language="pt-BR", 
                      language_name="Portuguese (Brazil)", gender="Female", provider="azure"),
                Voice(id="pt-PT-RaquelNeural", name="Raquel", language="pt-PT", 
                      language_name="Portuguese (Portugal)", gender="Female", provider="azure"),
                
                # Italian voices
                Voice(id="it-IT-ElsaNeural", name="Elsa", language="it-IT", 
                      language_name="Italian", gender="Female", provider="azure"),
                Voice(id="it-IT-DiegoNeural", name="Diego", language="it-IT", 
                      language_name="Italian", gender="Male", provider="azure"),
            ]
            self._voices_cache = voices
        
        # Filter by language if specified
        if language:
            lang_prefix = language.lower()
            voices = [v for v in voices if v.language.lower().startswith(lang_prefix)]
        
        return voices
    
    def health_check(self) -> bool:
        """Check Azure TTS health."""
        try:
            speech_config = self._get_client()
            return speech_config is not None and self.settings.azure_speech_key is not None
        except Exception as e:
            logger.error(f"Azure TTS health check failed: {e}")
            return False
