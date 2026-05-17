"""TTS API endpoints."""

from typing import Optional, List
from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import StreamingResponse, JSONResponse
from loguru import logger

from ..schemas import (
    SynthesizeRequest,
    StreamRequest,
    Voice,
    ProviderInfo,
    HealthResponse,
    ErrorResponse,
    OutputFormat,
)
from ..providers import get_provider
from ..config import get_settings

router = APIRouter(prefix="/tts", tags=["TTS"])


def get_current_provider():
    """Get the current TTS provider instance."""
    settings = get_settings()
    try:
        return get_provider(settings.tts_provider.value)
    except Exception as e:
        logger.error(f"Failed to get TTS provider: {e}")
        raise HTTPException(status_code=503, detail=f"TTS provider unavailable: {e}")


@router.post(
    "/synthesize",
    summary="Synthesize speech",
    description="Convert text to speech and return audio file",
    responses={
        200: {"content": {"audio/mpeg": {}, "audio/wav": {}, "audio/ogg": {}}},
        400: {"model": ErrorResponse},
        503: {"model": ErrorResponse},
    },
)
async def synthesize(request: SynthesizeRequest):
    """Synthesize text to speech.
    
    Args:
        request: Synthesis parameters including text, voice, and audio settings.
    
    Returns:
        Audio file in the requested format.
    """
    try:
        provider = get_current_provider()
        
        # Use default voice if not specified
        voice = request.voice
        language = request.language
        speed = request.speed
        pitch = request.pitch
        
        logger.debug(f"Synthesizing text: {request.text[:50]}... with voice: {voice}")
        
        # Synthesize audio
        audio_data = provider.synthesize(
            text=request.text,
            voice=voice,
            language=language,
            speed=speed,
            pitch=pitch,
            output_format=request.output_format,
        )
        
        # Determine media type
        media_types = {
            OutputFormat.MP3: "audio/mpeg",
            OutputFormat.WAV: "audio/wav",
            OutputFormat.OGG: "audio/ogg",
            OutputFormat.FLAC: "audio/flac",
        }
        media_type = media_types.get(request.output_format, "audio/mpeg")
        
        # Generate filename
        filename = f"speech_{hash(request.text) % 10000}.{request.output_format.value}"
        
        return StreamingResponse(
            iter([audio_data]),
            media_type=media_type,
            headers={
                "Content-Disposition": f'attachment; filename="{filename}"',
                "Content-Length": str(len(audio_data)),
            }
        )
        
    except Exception as e:
        logger.error(f"Synthesis failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post(
    "/stream",
    summary="Stream speech synthesis",
    description="Stream synthesized speech in real-time",
    responses={
        200: {"content": {"audio/mpeg": {}, "audio/wav": {}, "audio/ogg": {}}},
        400: {"model": ErrorResponse},
        503: {"model": ErrorResponse},
    },
)
async def stream_synthesize(request: StreamRequest):
    """Stream synthesized speech in real-time.
    
    Args:
        request: Stream parameters.
    
    Returns:
        Streaming audio chunks.
    """
    try:
        provider = get_current_provider()
        
        logger.debug(f"Streaming text: {request.text[:50]}...")
        
        async def generate():
            async for chunk in provider.stream(
                text=request.text,
                voice=request.voice,
                language=request.language,
                speed=request.speed,
                output_format=request.output_format,
            ):
                yield chunk
        
        # Determine media type
        media_types = {
            OutputFormat.MP3: "audio/mpeg",
            OutputFormat.WAV: "audio/wav",
            OutputFormat.OGG: "audio/ogg",
            OutputFormat.FLAC: "audio/flac",
        }
        media_type = media_types.get(request.output_format, "audio/mpeg")
        
        return StreamingResponse(
            generate(),
            media_type=media_type,
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "Transfer-Encoding": "chunked",
            }
        )
        
    except Exception as e:
        logger.error(f"Streaming synthesis failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get(
    "/voices",
    summary="List voices",
    description="Get list of available voices for the current provider",
    response_model=List[Voice],
)
async def list_voices(language: Optional[str] = Query(None, description="Filter by language code")):
    """List available voices.
    
    Args:
        language: Optional language filter.
    
    Returns:
        List of available voices.
    """
    try:
        provider = get_current_provider()
        voices = provider.list_voices(language=language)
        return voices
    except Exception as e:
        logger.error(f"Failed to list voices: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get(
    "/providers",
    summary="List providers",
    description="Get information about available TTS providers",
    response_model=List[ProviderInfo],
)
async def list_providers():
    """List all available TTS providers.
    
    Returns:
        List of provider information.
    """
    providers = [
        ProviderInfo(
            name="azure",
            display_name="Azure Cognitive Services TTS",
            supported_languages=[
                "en-US", "en-GB", "en-AU", "en-CA", "en-IN",
                "zh-CN", "zh-TW", "zh-HK",
                "ja-JP", "ko-KR",
                "fr-FR", "de-DE", "es-ES", "es-MX",
                "pt-BR", "pt-PT", "it-IT", "ru-RU", "nl-NL",
            ],
            features=[
                "Neural voices",
                "Multi-language support",
                "Voice styles",
                "Pitch/speed adjustment",
                "SSML support",
            ]
        ),
        ProviderInfo(
            name="google",
            display_name="Google Cloud Text-to-Speech",
            supported_languages=[
                "en-US", "en-GB", "en-AU",
                "zh-CN", "zh-TW",
                "ja-JP", "ko-KR",
                "fr-FR", "de-DE", "es-ES", "it-IT",
                "pt-BR", "ru-RU", "nl-NL",
            ],
            features=[
                "WaveNet voices",
                "DeepMind WaveNet technology",
                "Multi-language support",
                "SSML support",
                "Voice selection",
            ]
        ),
        ProviderInfo(
            name="elevenlabs",
            display_name="ElevenLabs",
            supported_languages=[
                "en", "zh", "ja", "ko", "de", "fr",
                "es", "it", "pt", "pl", "nl", "ar",
            ],
            features=[
                "Ultra-realistic AI voices",
                "Voice cloning",
                "Emotion control",
                "Voice conversion",
                "Multi-language (28+ languages)",
                "Streaming API",
            ]
        ),
        ProviderInfo(
            name="coqui",
            display_name="Coqui TTS (Local)",
            supported_languages=["en", "multi"],
            features=[
                "Open-source",
                "Local deployment",
                "Privacy-focused",
                "Voice conversion",
                "Custom model support",
            ]
        ),
        ProviderInfo(
            name="edge",
            display_name="Microsoft Edge TTS (Neural)",
            supported_languages=[
                "zh-CN", "zh-TW", "zh-HK",
                "en-US", "en-GB", "en-AU", "en-CA",
                "ja-JP", "ko-KR",
                "fr-FR", "de-DE", "es-ES", "es-MX",
                "pt-BR", "pt-PT", "it-IT", "ru-RU",
                "hi-IN",
            ],
            features=[
                "Neural voices (Neural suffix)",
                "No API key required",
                "Multi-language (50+ languages)",
                "Speed/pitch adjustment",
                "Local TTS (no cloud dependency)",
            ]
        ),
    ]

    return providers


@router.get(
    "/provider",
    summary="Get current provider",
    description="Get information about the currently active TTS provider",
    response_model=ProviderInfo,
)
async def get_provider_info():
    """Get current provider information."""
    settings = get_settings()
    
    providers_map = {
        "azure": ProviderInfo(
            name="azure",
            display_name="Azure Cognitive Services TTS",
            supported_languages=["en-US", "en-GB", "zh-CN", "ja-JP", "ko-KR", "fr-FR", "de-DE", "es-ES", "pt-BR", "it-IT"],
            features=["Neural voices", "Multi-language support", "Voice styles", "SSML support"],
        ),
        "google": ProviderInfo(
            name="google",
            display_name="Google Cloud Text-to-Speech",
            supported_languages=["en-US", "en-GB", "zh-CN", "ja-JP", "ko-KR", "fr-FR", "de-DE", "es-ES", "it-IT"],
            features=["WaveNet voices", "DeepMind technology", "SSML support"],
        ),
        "elevenlabs": ProviderInfo(
            name="elevenlabs",
            display_name="ElevenLabs",
            supported_languages=["en", "zh", "ja", "ko", "de", "fr", "es", "it", "pt"],
            features=["Ultra-realistic voices", "Voice cloning", "Emotion control", "Voice conversion"],
        ),
        "coqui": ProviderInfo(
            name="coqui",
            display_name="Coqui TTS (Local)",
            supported_languages=["en", "multi"],
            features=["Open-source", "Local deployment", "Privacy-focused"],
        ),
        "edge": ProviderInfo(
            name="edge",
            display_name="Microsoft Edge TTS (Neural)",
            supported_languages=["zh-CN", "zh-TW", "en-US", "en-GB", "ja-JP", "ko-KR", "fr-FR", "de-DE", "es-ES", "pt-BR"],
            features=["Neural voices", "No API key required", "Multi-language", "Speed/pitch adjustment"],
        ),
    }

    return providers_map.get(settings.tts_provider.value, providers_map["azure"])


@router.get(
    "/health",
    summary="Health check",
    description="Check TTS service health status",
    response_model=HealthResponse,
)
async def health_check():
    """Check TTS service health."""
    settings = get_settings()
    
    try:
        provider = get_current_provider()
        provider_healthy = provider.health_check()
        
        return HealthResponse(
            status="healthy" if provider_healthy else "degraded",
            provider=settings.tts_provider.value,
            provider_status="healthy" if provider_healthy else "unhealthy",
            version="0.1.0",
        )
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return HealthResponse(
            status="unhealthy",
            provider=settings.tts_provider.value,
            provider_status="error",
            version="0.1.0",
        )
