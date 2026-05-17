"""TTS Tools for AI Agents Service."""

import httpx
from typing import Optional, List

from langchain_core.tools import tool


TTS_SERVICE_URL = "http://localhost:8013"


@tool("tts_synthesize")
def tts_synthesize(
    text: str,
    voice: Optional[str] = None,
    language: Optional[str] = None,
    speed: float = 1.0,
    pitch: float = 0,
    output_format: str = "mp3"
) -> str:
    """Synthesize text to speech.
    
    Converts text input into spoken audio. Use this when you need to:
    - Convert text responses to speech
    - Generate audio versions of content
    - Create voice output for user interfaces
    
    Args:
        text: The text to synthesize into speech. Should be clear and complete sentences.
        voice: Optional voice identifier (e.g., 'en-US-JennyNeural' for Azure).
               If not provided, uses the default voice.
        language: Optional language code (e.g., 'en-US', 'zh-CN', 'ja-JP').
                  If not provided, uses the default language.
        speed: Speech speed multiplier. 1.0 is normal speed.
               Range: 0.25 to 4.0
        pitch: Voice pitch adjustment in Hz. 0 is normal pitch.
               Range: -20 to +20
        output_format: Audio output format. Options: 'mp3', 'wav', 'ogg'
                      Default: 'mp3'
    
    Returns:
        A message confirming synthesis completion with audio details.
        Note: The actual audio is returned as binary data.
    """
    try:
        with httpx.Client(timeout=60.0) as client:
            response = client.post(
                f"{TTS_SERVICE_URL}/tts/synthesize",
                json={
                    "text": text,
                    "voice": voice,
                    "language": language,
                    "speed": speed,
                    "pitch": pitch,
                    "output_format": output_format
                }
            )
            
            if response.status_code == 200:
                audio_size = len(response.content)
                return f"""Speech synthesis completed successfully.

Audio Details:
- Text length: {len(text)} characters
- Audio size: {audio_size:,} bytes ({audio_size / 1024:.1f} KB)
- Format: {output_format.upper()}
- Voice: {voice or 'default'}
- Language: {language or 'default'}
- Speed: {speed}x
- Pitch: {pitch:+d} Hz

The synthesized audio has been generated."""
            else:
                return f"Speech synthesis failed with status code: {response.status_code}\n{response.text}"
                
    except httpx.ConnectError:
        return "Error: Cannot connect to TTS service. Please ensure the TTS service is running on port 8013."
    except Exception as e:
        return f"Error during speech synthesis: {str(e)}"


@tool("tts_list_voices")
def tts_list_voices(language: Optional[str] = None) -> str:
    """List available TTS voices.
    
    Retrieves all available voices from the TTS service.
    Use this to find appropriate voices for different languages and use cases.
    
    Args:
        language: Optional language code filter (e.g., 'en-US', 'zh-CN', 'ja-JP').
                   If not provided, returns all voices.
    
    Returns:
        A formatted list of available voices with their details.
    """
    try:
        params = {}
        if language:
            params["language"] = language
        
        with httpx.Client(timeout=30.0) as client:
            response = client.get(
                f"{TTS_SERVICE_URL}/tts/voices",
                params=params
            )
            
            if response.status_code == 200:
                voices = response.json()
                
                if not voices:
                    return "No voices found matching the criteria."
                
                # Group by language
                by_language = {}
                for voice in voices:
                    lang = voice.get("language", "unknown")
                    if lang not in by_language:
                        by_language[lang] = []
                    by_language[lang].append(voice)
                
                output = f"Available TTS Voices ({len(voices)} total)\n"
                if language:
                    output = f"Available TTS Voices for '{language}' ({len(voices)} voices)\n"
                output += "=" * 60 + "\n\n"
                
                for lang, lang_voices in sorted(by_language.items()):
                    output += f"## {lang}\n"
                    for v in lang_voices:
                        default_marker = " [DEFAULT]" if v.get("is_default") else ""
                        gender = f" ({v.get('gender', 'unknown').lower()})" if v.get("gender") else ""
                        output += f"- **{v['name']}** - ID: `{v['id']}`{gender}{default_marker}\n"
                    output += "\n"
                
                return output
            else:
                return f"Failed to list voices. Status: {response.status_code}"
                
    except httpx.ConnectError:
        return "Error: Cannot connect to TTS service. Please ensure the TTS service is running on port 8013."
    except Exception as e:
        return f"Error listing voices: {str(e)}"


@tool("tts_stream")
def tts_stream(
    text: str,
    voice: Optional[str] = None,
    language: Optional[str] = None,
    speed: float = 1.0,
    output_format: str = "mp3"
) -> str:
    """Stream synthesized speech in real-time.
    
    Similar to synthesize but streams the audio data in chunks.
    Useful for real-time applications or when immediate playback is needed.
    
    Args:
        text: The text to synthesize.
        voice: Optional voice identifier.
        language: Optional language code.
        speed: Speech speed multiplier (0.25 to 4.0).
        output_format: Audio format ('mp3', 'wav', 'ogg').
    
    Returns:
        Status message about the streaming operation.
    """
    try:
        with httpx.Client(timeout=120.0) as client:
            response = client.post(
                f"{TTS_SERVICE_URL}/tts/stream",
                json={
                    "text": text,
                    "voice": voice,
                    "language": language,
                    "speed": speed,
                    "output_format": output_format
                },
                timeout=120.0
            )
            
            if response.status_code == 200:
                total_bytes = 0
                for chunk in response.iter_bytes(chunk_size=8192):
                    total_bytes += len(chunk)
                
                return f"""Speech streaming completed.

Stream Details:
- Text length: {len(text)} characters
- Total audio: {total_bytes:,} bytes ({total_bytes / 1024:.1f} KB)
- Format: {output_format.upper()}
- Voice: {voice or 'default'}

Audio chunks have been streamed successfully."""
            else:
                return f"Speech streaming failed with status code: {response.status_code}"
                
    except httpx.ConnectError:
        return "Error: Cannot connect to TTS service."
    except Exception as e:
        return f"Error during speech streaming: {str(e)}"


@tool("tts_get_providers")
def tts_get_providers() -> str:
    """Get information about available TTS providers.
    
    Returns details about each supported TTS provider including:
    - Supported languages
    - Available features
    - Current configuration
    
    Returns:
        Formatted information about all TTS providers.
    """
    try:
        with httpx.Client(timeout=30.0) as client:
            response = client.get(f"{TTS_SERVICE_URL}/tts/providers")
            
            if response.status_code == 200:
                providers = response.json()
                
                output = "Available TTS Providers\n"
                output += "=" * 60 + "\n\n"
                
                for p in providers:
                    output += f"## {p['display_name']}\n"
                    output += f"- Provider ID: `{p['name']}`\n"
                    output += f"- Languages: {len(p['supported_languages'])} supported\n"
                    output += "- Supported Languages:\n"
                    for lang in p['supported_languages'][:10]:
                        output += f"  - {lang}\n"
                    if len(p['supported_languages']) > 10:
                        output += f"  - ... and {len(p['supported_languages']) - 10} more\n"
                    output += "- Features:\n"
                    for feature in p['features']:
                        output += f"  - {feature}\n"
                    output += "\n"
                
                return output
            else:
                return f"Failed to get providers. Status: {response.status_code}"
                
    except httpx.ConnectError:
        return "Error: Cannot connect to TTS service."
    except Exception as e:
        return f"Error getting providers: {str(e)}"


def get_all_tts_tools():
    """Get all TTS tools.
    
    Returns:
        List of all TTS-related LangChain tools.
    """
    return [
        tts_synthesize,
        tts_list_voices,
        tts_stream,
        tts_get_providers,
    ]
